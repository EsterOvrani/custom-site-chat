package com.example.backend.query.service;

import com.example.backend.collection.service.CollectionService;
import com.example.backend.query.dto.PublicQueryRequest;
import com.example.backend.query.dto.QueryResponse;  
import com.example.backend.user.model.User;
import com.example.backend.common.infrastructure.vectordb.QdrantVectorService;
import com.example.backend.common.exception.UnauthorizedException;
import com.example.backend.user.service.TokenService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryService {

    private final CollectionService collectionService;
    private final QdrantVectorService qdrantVectorService;
    private final EmbeddingModel embeddingModel;
    private final OpenAiChatModel chatModel;
    private final PromptService promptService;
    private final TokenService tokenService;

    private static final int MAX_RELEVANT_CHUNKS = 5;
    private static final int MAX_HISTORY_MESSAGES = 10;

    // Search documents and generate AI answer
    public QueryResponse askQuestion(String secretKey, String question, List<PublicQueryRequest.HistoryMessage> history) {
        
        long startTime = System.currentTimeMillis();
        int rewriteTokens = 0;

        try {
            // 1. Verify secretKey
            User user = collectionService.validateSecretKey(secretKey);
            
            // 2. Limit history messages
            List<PublicQueryRequest.HistoryMessage> validatedHistory = 
                validateAndLimitHistory(history);
            
            log.info("Query from user {} with {} history messages", 
                user.getId(), 
                validatedHistory.size());

            // 3. Improved query construction (rewritten with LLM!)
            RewriteResult rewriteResult = buildEnhancedQuery(question, validatedHistory);
            String enhancedQuery = rewriteResult.getQuery();
            rewriteTokens = rewriteResult.getTokensUsed();
            log.info("🔍 Searching with query: '{}', rewrite tokens: {}", enhancedQuery, rewriteTokens);

            // 4. Search for relevant documents (with the rewritten question!)
            List<RelevantDocument> relevantDocs = searchRelevantDocuments(
                user.getCollectionName(),
                enhancedQuery, // Here the rewritten question is used!
                validatedHistory
            );

            // 5. If no relevant documents were found
            if (relevantDocs.isEmpty()) {
                // Still consume rewrite tokens even if no docs found
                if (rewriteTokens > 0) {
                    try {
                        tokenService.consumeTokens(user, rewriteTokens);
                        log.info("💰 Consumed {} rewrite tokens (no docs found) for user {}", 
                            rewriteTokens, user.getId());
                    } catch (Exception e) {
                        log.error("Failed to consume rewrite tokens", e);
                    }
                }
                return createNoResultsResponse(question, enhancedQuery, startTime, rewriteTokens);
            }

            // 6. Building messages with history
            List<ChatMessage> messages = buildMessagesWithHistory(
                question,
                relevantDocs,
                validatedHistory
            );

        // 7. Sending to GPT
        Response<AiMessage> response = chatModel.generate(messages);
        String answer = response.content().text();

        // 8. Calculating metrics and tokens
        long responseTime = System.currentTimeMillis() - startTime;
        Double confidence = calculateConfidence(relevantDocs);

        OpenAiTokenizer tokenizer = new OpenAiTokenizer("gpt-4");
        int outputTokens = tokenizer.estimateTokenCountInMessage(response.content());

        // 8.1. Count input tokens as well
        int inputTokens = messages.stream()
            .mapToInt(msg -> tokenizer.estimateTokenCountInMessage(msg))
            .sum();

        // 8.2. Total = rewrite + main query (input + output)
        int totalTokens = rewriteTokens + inputTokens + outputTokens;

        // 8.3. Consume tokens from user quota
        try {
            tokenService.consumeTokens(user, totalTokens);
            log.info("💰 Consumed {} tokens (rewrite: {}, input: {}, output: {}) for user {}", 
                totalTokens, rewriteTokens, inputTokens, outputTokens, user.getId());
        } catch (Exception e) {
            log.error("Failed to consume tokens", e);
            // Continue anyway - we already generated the response
        }

        // 9. Building sources
        List<QueryResponse.Source> sources = buildSources(relevantDocs);

        return QueryResponse.builder()
            .answer(answer)
            .rewrittenQuery(enhancedQuery)  
            .sources(sources)
            .confidence(confidence)
            .tokensUsed(totalTokens) // Return total tokens including rewrite
            .responseTimeMs(responseTime)
            .build();

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to answer question", e);
            throw new RuntimeException("Error processing question: " + e.getMessage());
        }
    }

    // Limit history messages
    private List<PublicQueryRequest.HistoryMessage> validateAndLimitHistory(List<PublicQueryRequest.HistoryMessage> history) {
        
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Limit to MAX_HISTORY_MESSAGES latest messages
        if (history.size() > MAX_HISTORY_MESSAGES) {
            log.warn("History too long: {} messages, limiting to {}", 
                history.size(), MAX_HISTORY_MESSAGES);
            
            return history.subList(
                history.size() - MAX_HISTORY_MESSAGES, 
                history.size()
            );
        }
        
        return history;
    }

    // Find relevant chunks in Qdrant
    private List<RelevantDocument> searchRelevantDocuments(
            String collectionName,
            String enhancedQuery,
            List<PublicQueryRequest.HistoryMessage> history) {
        
        try {
            EmbeddingStore<TextSegment> embeddingStore = 
                qdrantVectorService.getEmbeddingStoreForCollection(collectionName);
            
            if (embeddingStore == null) {
                log.error("❌ No embedding store for collection: {}", collectionName);
                return new ArrayList<>();
            }

            log.info("🔍 Searching Qdrant with enhanced query: '{}'", enhancedQuery);       

            // Convert to vector (temporary!)
            Embedding queryEmbedding = embeddingModel.embed(enhancedQuery).content();

            // search
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(MAX_RELEVANT_CHUNKS)
                .minScore(0.5)
                .build();

            EmbeddingSearchResult<TextSegment> searchResult = 
                embeddingStore.search(searchRequest);

            // Convert to a list of documents
            List<RelevantDocument> relevantDocs = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
                RelevantDocument doc = new RelevantDocument();
                doc.setText(match.embedded().text());
                doc.setScore(match.score());
                
                if (match.embedded().metadata() != null) {
                    String docName = match.embedded().metadata()
                        .getString("document_name");
                    doc.setDocumentName(docName != null ? docName : "Unknown");
                }
                
                relevantDocs.add(doc);
            }

            log.info("✅ Found {} relevant chunks", relevantDocs.size());
            return relevantDocs;

        } catch (Exception e) {
            log.error("❌ Failed to search documents", e);
            return new ArrayList<>();
        }
    }


    // Improved query builder - rewrites with LLM to be independent
    private RewriteResult buildEnhancedQuery(String question, List<PublicQueryRequest.HistoryMessage> history) {
        
        // If no history - return original question with 0 tokens
        if (history == null || history.isEmpty()) {
            log.info("📝 No history - using original question");
            return new RewriteResult(question, 0);
        }
        
        // Rewrite query with LLM
        return rewriteQueryWithLLM(question, history);
    }

    // Rewrite the query with LLM to be independent, Uses all available history (up to 10 messages)
    private RewriteResult rewriteQueryWithLLM(String question, List<PublicQueryRequest.HistoryMessage> history) {
        
        try {
            log.info("🔄 Rewriting query with LLM...");
            long startTime = System.currentTimeMillis();
            
            // Build context from all history (already limited to 10 messages!)
            StringBuilder contextBuilder = new StringBuilder();
            
            for (PublicQueryRequest.HistoryMessage msg : history) {
                String role = msg.isUser() ? "User" : "Assistant";
                contextBuilder.append(role).append(": ").append(msg.getContent()).append("\n");
            }
            
            log.info("📚 Using {} history messages for rewriting", history.size());
            
            // Build rewrite prompt
            String rewritePrompt = buildRewritePrompt(contextBuilder.toString(), question);
            
            // Build messages for token counting
            SystemMessage systemMsg = SystemMessage.from("You are a query rewriting assistant. Your ONLY job is to rewrite user questions to be standalone based on conversation history.");
            UserMessage userMsg = UserMessage.from(rewritePrompt);
            
            // Send to LLM
            log.info("🚀 Sending rewrite request to LLM...");
            
            Response<AiMessage> response = chatModel.generate(systemMsg, userMsg);
            
            String rewrittenQuery = response.content().text().trim();
            
            // Clean the response - remove quotes and prefixes
            rewrittenQuery = rewrittenQuery
                .replaceAll("^\"|\"$", "")
                .replaceAll("^(Rewritten question:|Rewritten question)\\s*", "")
                .trim();
            
            // Calculate tokens used
            OpenAiTokenizer tokenizer = new OpenAiTokenizer("gpt-4");
            int inputTokens = tokenizer.estimateTokenCountInMessage(systemMsg) 
                            + tokenizer.estimateTokenCountInMessage(userMsg);
            int outputTokens = tokenizer.estimateTokenCountInMessage(response.content());
            int totalTokens = inputTokens + outputTokens;
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("✅ Query rewriting completed in {}ms, tokens: {}", duration, totalTokens);
            log.info("📥 Original:  '{}'", question);
            log.info("📤 Rewritten: '{}'", rewrittenQuery);
            
            return new RewriteResult(rewrittenQuery, totalTokens);
            
        } catch (Exception e) {
            log.error("❌ Failed to rewrite query - using original question", e);
            // In case of error - return original question with 0 tokens
            return new RewriteResult(question, 0);
        }
    }

    // Constructs a prompt to rewrite the query according to the language
    private String buildRewritePrompt(String context, String question) {
        String detectedLanguage = detectLanguage(question);
        return promptService.getQueryRewritePrompt(detectedLanguage, context, question);
    }

    // Build chat messages for GPT
    private List<ChatMessage> buildMessagesWithHistory(String question, List<RelevantDocument> relevantDocs, List<PublicQueryRequest.HistoryMessage> history) {
        
        List<ChatMessage> messages = new ArrayList<>();

        // 1. System message from PromptService
        String detectedLanguage = detectLanguage(question);
        String languageName = detectedLanguage.equals("he") ? "Hebrew" : "English";
        
        String systemMessage = promptService.getSystemMessage(languageName);
        messages.add(SystemMessage.from(systemMessage));

        // 2. Adding history (plain text!)
        if (history != null && !history.isEmpty()) {
            log.info("Adding {} history messages", history.size());
            
            for (PublicQueryRequest.HistoryMessage msg : history) {
                if ("user".equals(msg.getRole())) {
                    messages.add(UserMessage.from(msg.getContent()));
                } else if ("assistant".equals(msg.getRole())) {
                    messages.add(AiMessage.from(msg.getContent()));
                }
            }
        }

        // 3. The context from the documents + the new question
        StringBuilder context = new StringBuilder();
        context.append("Relevant information from documents:\n\n");
        
        for (int i = 0; i < relevantDocs.size(); i++) {
            RelevantDocument doc = relevantDocs.get(i);
            context.append(String.format("[Document %d - %s]:\n%s\n\n",
                i + 1,
                doc.getDocumentName(),
                doc.getText()
            ));
        }

        messages.add(UserMessage.from(
            context.toString() + 
            "\nUser Question (" + languageName + "): " + question
        ));

        return messages;
    }

    // Detect Hebrew or English
    private String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "en";
        }
        
        int hebrewChars = 0;
        int totalChars = 0;
        
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                totalChars++;
                if (c >= '\u0590' && c <= '\u05FF') {
                    hebrewChars++;
                }
            }
        }
        
        if (totalChars > 0 && (hebrewChars * 100.0 / totalChars) > 30) {
            return "he";
        }
        
        return "en";
    }

   // Convert results to source DTOs
    private List<QueryResponse.Source> buildSources(List<RelevantDocument> relevantDocs) {
        
        List<QueryResponse.Source> sources = new ArrayList<>();
        for (int i = 0; i < relevantDocs.size(); i++) {
            RelevantDocument doc = relevantDocs.get(i);
            QueryResponse.Source source = QueryResponse.Source.builder()
                .documentName(doc.getDocumentName())
                .excerpt(truncateText(doc.getText(), 200))
                .relevanceScore(doc.getScore())
                .isPrimary(i == 0)
                .build();
            sources.add(source);
        }
        return sources;
    }

    // Average relevance scores
    private Double calculateConfidence(List<RelevantDocument> results) {
        if (results.isEmpty()) {
            return 0.0;
        }
        double avgScore = results.stream()
            .mapToDouble(RelevantDocument::getScore)
            .average()
            .orElse(0.0);
        return Math.min(avgScore, 1.0);
    }

    // Truncate text to max length
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    // Response when no documents found
    private QueryResponse createNoResultsResponse(String originalQuestion, String enhancedQuery, long startTime, int tokensUsed) {
        
        String detectedLanguage = detectLanguage(originalQuestion);
        String message = promptService.getNoResultsMessage(detectedLanguage);

        long responseTime = System.currentTimeMillis() - startTime;

        return QueryResponse.builder()
            .answer(message)
            .rewrittenQuery(enhancedQuery)
            .sources(Collections.emptyList())
            .confidence(0.0)
            .tokensUsed(tokensUsed)
            .responseTimeMs(responseTime)
            .build();
    }

    // Inner class
    @lombok.Data
    private static class RelevantDocument {
        private String text;
        private Double score;
        private String documentName;
    }

    // Inner class for rewrite result with tokens
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class RewriteResult {
        private String query;
        private int tokensUsed;
    }
}