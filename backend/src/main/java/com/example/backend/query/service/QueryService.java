package com.example.backend.query.service;

import com.example.backend.collection.service.CollectionService;
import com.example.backend.query.dto.PublicQueryRequest;
import com.example.backend.query.dto.QueryResponse;  
import com.example.backend.user.model.User;
import com.example.backend.common.infrastructure.vectordb.QdrantVectorService;
import com.example.backend.common.exception.UnauthorizedException;
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

    private static final int MAX_RELEVANT_CHUNKS = 5;
    private static final int MAX_HISTORY_MESSAGES = 10;

    // Search documents and generate AI answer
    public QueryResponse askQuestion(String secretKey, String question, List<PublicQueryRequest.HistoryMessage> history) {
        
        long startTime = System.currentTimeMillis();

        try {
            // 1. Verify secretKey
            User user = collectionService.validateSecretKey(secretKey);
            
            // 2. Limit history messages
            List<PublicQueryRequest.HistoryMessage> validatedHistory = 
                validateAndLimitHistory(history);
            
            log.info("Query from user {} with {} history messages", 
                user.getId(), 
                validatedHistory.size());

            // 3. Search for relevant documents (with context from history)
            List<RelevantDocument> relevantDocs = searchRelevantDocuments(
                user.getCollectionName(),
                question,
                validatedHistory
            );

            if (relevantDocs.isEmpty()) {
                return createNoResultsResponse(question, startTime);
            }

            // 4. Building messages for GPT (with history!)
            List<ChatMessage> messages = buildMessagesWithHistory(
                question,
                relevantDocs,
                validatedHistory
            );

            // 5. Sending to GPT
            Response<AiMessage> response = chatModel.generate(messages);
            String answer = response.content().text();

            // 6. Calculating metrics
            long responseTime = System.currentTimeMillis() - startTime;
            Double confidence = calculateConfidence(relevantDocs);
            
            OpenAiTokenizer tokenizer = new OpenAiTokenizer("gpt-4");
            int tokensUsed = tokenizer.estimateTokenCountInMessage(response.content());

            // 7. Building sources
            List<QueryResponse.Source> sources = buildSources(relevantDocs);

            return QueryResponse.builder()
                .answer(answer)
                .sources(sources)
                .confidence(confidence)
                .tokensUsed(tokensUsed)
                .responseTimeMs(responseTime)
                .build();

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to answer question", e);
            throw new RuntimeException("שגיאה בעיבוד השאלה: " + e.getMessage());
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
            String question,
            List<PublicQueryRequest.HistoryMessage> history) {
        
        try {
            EmbeddingStore<TextSegment> embeddingStore = 
                qdrantVectorService.getEmbeddingStoreForCollection(collectionName);
            
            if (embeddingStore == null) {
                log.error("❌ No embedding store for collection: {}", collectionName);
                return new ArrayList<>();
            }

            // Build an improved query with the context
            String enhancedQuery = buildEnhancedQuery(question, history);
            log.info("Enhanced query: {}", enhancedQuery);

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

    // Add context from history to query
    private String buildEnhancedQuery(String question, List<PublicQueryRequest.HistoryMessage> history) {
        
        if (history == null || history.isEmpty()) {
            return question;
        }
        
        // Get the user's last messages (if any)
        StringBuilder contextBuilder = new StringBuilder();
        int userMessagesAdded = 0;
        
        for (int i = history.size() - 1; i >= 0 && userMessagesAdded < 2; i--) {
            PublicQueryRequest.HistoryMessage msg = history.get(i);
            if ("user".equals(msg.getRole())) {
                contextBuilder.insert(0, msg.getContent() + " ");
                userMessagesAdded++;
            }
        }
        
        // Add the current question
        contextBuilder.append(question);
        
        return contextBuilder.toString();
    }

    // Build chat messages for GPT
    private List<ChatMessage> buildMessagesWithHistory(String question, List<RelevantDocument> relevantDocs, List<PublicQueryRequest.HistoryMessage> history) {
        
        List<ChatMessage> messages = new ArrayList<>();

        // 1. System message
        String detectedLanguage = detectLanguage(question);
        String languageName = detectedLanguage.equals("he") ? "Hebrew" : "English";
        
        messages.add(SystemMessage.from(
            "You are a helpful AI assistant.\n" +
            "ANSWER IN " + languageName.toUpperCase() + " ONLY!\n" +
            "Base your answer ONLY on the provided documents.\n" +
            "Be natural and conversational.\n"
        ));

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
    private QueryResponse createNoResultsResponse(String question, long startTime) {
        String detectedLanguage = detectLanguage(question);
        String message = detectedLanguage.equals("he") 
            ? "מצטער, לא מצאתי מידע רלוונטי במסמכים."
            : "Sorry, I couldn't find relevant information in the documents.";

        long responseTime = System.currentTimeMillis() - startTime;

        return QueryResponse.builder()
            .answer(message)
            .sources(Collections.emptyList())
            .confidence(0.0)
            .tokensUsed(0)
            .responseTimeMs(responseTime)
            .build();
    }

    // ✅ Inner class
    @lombok.Data
    private static class RelevantDocument {
        private String text;
        private Double score;
        private String documentName;
    }
}