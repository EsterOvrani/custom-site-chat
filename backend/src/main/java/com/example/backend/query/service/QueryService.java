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

            // 3. Improved query construction (rewritten with LLM!)
            String enhancedQuery = buildEnhancedQuery(question, validatedHistory);
            log.info("🔍 Searching with query: '{}'", enhancedQuery);

            // 4. Search for relevant documents (with the rewritten question!)
            List<RelevantDocument> relevantDocs = searchRelevantDocuments(
                user.getCollectionName(),
                enhancedQuery, // Here the rewritten question is used!
                validatedHistory
            );

            // 5. If no relevant documents were found
            if (relevantDocs.isEmpty()) {
                return createNoResultsResponse(question, enhancedQuery, startTime);  // send also enhancedQuery
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

            // 8. Calculating metrics
            long responseTime = System.currentTimeMillis() - startTime;
            Double confidence = calculateConfidence(relevantDocs);
            
            OpenAiTokenizer tokenizer = new OpenAiTokenizer("gpt-4");
            int tokensUsed = tokenizer.estimateTokenCountInMessage(response.content());

            // 9. Building sources
            List<QueryResponse.Source> sources = buildSources(relevantDocs);

            return QueryResponse.builder()
                .answer(answer)
                .rewrittenQuery(enhancedQuery)  
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
    private String buildEnhancedQuery(String question, List<PublicQueryRequest.HistoryMessage> history) {
        
        // אם אין היסטוריה - מחזיר את השאלה המקורית
        if (history == null || history.isEmpty()) {
            log.info("📝 No history - using original question");
            return question;
        }
        
        // שכתוב השאלה עם LLM
        return rewriteQueryWithLLM(question, history);
    }

    // Rewrite the query with LLM to be independent, Uses all available history (up to 10 messages)
    private String rewriteQueryWithLLM(String question, List<PublicQueryRequest.HistoryMessage> history) {
        
        try {
            log.info("🔄 Rewriting query with LLM...");
            long startTime = System.currentTimeMillis();
            
            // בניית הקשר מכל ההיסטוריה (כבר מוגבלת ל-10 הודעות!)
            StringBuilder contextBuilder = new StringBuilder();
            
            for (PublicQueryRequest.HistoryMessage msg : history) {
                String role = msg.isUser() ? "User" : "Assistant";
                contextBuilder.append(role).append(": ").append(msg.getContent()).append("\n");
            }
            
            log.info("📚 Using {} history messages for rewriting", history.size());
            
            // בניית ה-Prompt לשכתוב
            String rewritePrompt = buildRewritePrompt(contextBuilder.toString(), question);
            
            // שליחה ל-LLM
            log.info("🚀 Sending rewrite request to LLM...");
            
            Response<AiMessage> response = chatModel.generate(
                SystemMessage.from("You are a query rewriting assistant. Your ONLY job is to rewrite user questions to be standalone based on conversation history."),
                UserMessage.from(rewritePrompt)
            );
            
            String rewrittenQuery = response.content().text().trim();
            
            // ניקוי התשובה - הסרת מרכאות ו-prefixes
            rewrittenQuery = rewrittenQuery
                .replaceAll("^\"|\"$", "")
                .replaceAll("^(Rewritten question:|שאלה משוכתבת:)\\s*", "")
                .trim();
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("✅ Query rewriting completed in {}ms", duration);
            log.info("📥 Original:  '{}'", question);
            log.info("📤 Rewritten: '{}'", rewrittenQuery);
            
            return rewrittenQuery;
            
        } catch (Exception e) {
            log.error("❌ Failed to rewrite query - using original question", e);
            // במקרה של שגיאה - מחזיר את השאלה המקורית
            return question;
        }
    }

    // Constructs a prompt to rewrite the query according to the language
    private String buildRewritePrompt(String context, String question) {
        
        // זיהוי שפה (משתמש בפונקציה הקיימת)
        String detectedLanguage = detectLanguage(question);
        
        if (detectedLanguage.equals("he")) {
            // Prompt בעברית
            return String.format("""
                בהתבסס על ההקשר הבא מהשיחה, שכתב את השאלה האחרונה של המשתמש כך שתהיה עצמאית ומובנת ללא הקשר.
                
                הקשר מהשיחה:
                %s
                
                שאלה נוכחית של המשתמש: %s
                
                חוקים חשובים:
                1. אל תוסיף מידע שלא קיים בשאלה המקורית
                2. שמור על הכוונה והמשמעות המקורית של השאלה
                3. אם השאלה כבר עצמאית ומובנת מעצמה - החזר אותה בדיוק כמו שהיא
                4. אם השאלה מתייחסת להיסטוריה (למשל "ובשבת?", "כמה זה עולה?") - שכתב אותה להיות מלאה
                5. החזר רק את השאלה המשוכתבת, ללא הסברים או טקסט נוסף
                6. אל תשנה את השפה של השאלה
                
                דוגמאות:
                - אם השאלה היא "ובשבת?" ובהיסטוריה דובר על שעות פתיחה → "מה שעות הפתיחה בשבת?"
                - אם השאלה היא "כמה זה עולה?" ובהיסטוריה דובר על קורס → "כמה עולה הקורס?"
                - אם השאלה היא "מה שעות הפתיחה שלכם?" → "מה שעות הפתיחה שלכם?" (כבר עצמאית)
                
                שאלה משוכתבת:""", 
                context, 
                question
            );
        } else {
            // Prompt באנגלית
            return String.format("""
                Based on the following conversation context, rewrite the user's last question to be standalone and understandable without any prior context.
                
                Conversation context:
                %s
                
                User's current question: %s
                
                Important rules:
                1. Do NOT add information that doesn't exist in the original question
                2. Keep the original intent and meaning of the question
                3. If the question is already standalone and self-contained - return it exactly as is
                4. If the question references history (e.g., "on Saturday?", "how much is it?") - rewrite it to be complete
                5. Return ONLY the rewritten question, no explanations or additional text
                6. Do NOT change the language of the question
                
                Examples:
                - If question is "on Saturday?" and history discussed opening hours → "What are the opening hours on Saturday?"
                - If question is "how much is it?" and history discussed a course → "How much does the course cost?"
                - If question is "What are your opening hours?" → "What are your opening hours?" (already standalone)
                
                Rewritten question:""", 
                context, 
                question
            );
        }
    }

    // Build chat messages for GPT
    private List<ChatMessage> buildMessagesWithHistory(String question, List<RelevantDocument> relevantDocs, List<PublicQueryRequest.HistoryMessage> history) {
        
        List<ChatMessage> messages = new ArrayList<>();

        // 1. System message
        String detectedLanguage = detectLanguage(question);
        String languageName = detectedLanguage.equals("he") ? "Hebrew" : "English";
        messages.add(SystemMessage.from("""
            You are a helpful AI assistant.
            ANSWER IN %s ONLY!
            Base your answer ONLY on the provided documents.
            Be natural and conversational.
            If you do not have enough information to answer the customer’s question,
            do NOT say that you cannot, are unable to, or are not allowed to provide the information.
            Do NOT imply restrictions, permissions, or confidentiality.
            Instead, clearly state that you do not have the requested information
            or that the information is not available to you.
            Do not mention missing documents, knowledge bases, or internal sources.
            In such cases, direct the customer to customer support.
            If support contact details are available (email, phone number, or contact form),
            include them in your response.
            Keep the answer polite, clear, and professional.
            """.formatted(languageName.toUpperCase())
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
    private QueryResponse createNoResultsResponse(String originalQuestion, String enhancedQuery, long startTime) {
        
        String detectedLanguage = detectLanguage(originalQuestion);
        String message = detectedLanguage.equals("he") 
            ? "מצטער, אין לי את המידע הזה כרגע. ממליץ לפנות לשירות הלקוחות."
            : "Sorry, I don\'t have this information right now. Please contact customer support for assistance.";

        long responseTime = System.currentTimeMillis() - startTime;

        return QueryResponse.builder()
            .answer(message)
            .rewrittenQuery(enhancedQuery)  // ⭐ חדש!
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