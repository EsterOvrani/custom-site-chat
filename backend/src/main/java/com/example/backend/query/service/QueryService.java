// backend/src/main/java/com/example/backend/query/service/QueryService.java

package com.example.backend.query.service;

import com.example.backend.collection.service.CollectionService;
import com.example.backend.query.dto.PublicQueryRequest;
import com.example.backend.query.dto.QueryResponse;  // ⭐ שונה מ-AnswerResponse
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

    // ⭐ הוספנו את ה-method הזה!
    public QueryResponse askQuestion(
            String secretKey,
            String question,
            List<PublicQueryRequest.HistoryMessage> history) {
        
        long startTime = System.currentTimeMillis();

        try {
            // 1. אימות secretKey
            User user = collectionService.validateSecretKey(secretKey);
            
            // 2. הגבלת היסטוריה ל-10 הודעות
            List<PublicQueryRequest.HistoryMessage> validatedHistory = 
                validateAndLimitHistory(history);
            
            log.info("Query from user {} with {} history messages", 
                user.getId(), 
                validatedHistory.size());

            // 3. חיפוש מסמכים רלוונטיים (עם הקשר מההיסטוריה)
            List<RelevantDocument> relevantDocs = searchRelevantDocuments(
                user.getCollectionName(),
                question,
                validatedHistory
            );

            if (relevantDocs.isEmpty()) {
                return createNoResultsResponse(question, startTime);
            }

            // 4. בניית messages ל-GPT (עם היסטוריה!)
            List<ChatMessage> messages = buildMessagesWithHistory(
                question,
                relevantDocs,
                validatedHistory
            );

            // 5. שליחה ל-GPT
            Response<AiMessage> response = chatModel.generate(messages);
            String answer = response.content().text();

            // 6. חישוב מטריקות
            long responseTime = System.currentTimeMillis() - startTime;
            Double confidence = calculateConfidence(relevantDocs);
            
            OpenAiTokenizer tokenizer = new OpenAiTokenizer("gpt-4");
            int tokensUsed = tokenizer.estimateTokenCountInMessage(response.content());

            // 7. בניית sources
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

    // ✅ ולידציה והגבלת היסטוריה
    private List<PublicQueryRequest.HistoryMessage> validateAndLimitHistory(
            List<PublicQueryRequest.HistoryMessage> history) {
        
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        
        // הגבלה ל-10 הודעות אחרונות
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

    // ✅ חיפוש משופר עם הקשר
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

            // בנה שאילתא משופרת עם הקשר
            String enhancedQuery = buildEnhancedQuery(question, history);
            log.info("Enhanced query: {}", enhancedQuery);

            // המרה לווקטור (זמני!)
            Embedding queryEmbedding = embeddingModel.embed(enhancedQuery).content();

            // חיפוש
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(MAX_RELEVANT_CHUNKS)
                .minScore(0.5)
                .build();

            EmbeddingSearchResult<TextSegment> searchResult = 
                embeddingStore.search(searchRequest);

            // המרה לרשימת מסמכים
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

    // ✅ בניית שאילתא משופרת עם הקשר מההיסטוריה
    private String buildEnhancedQuery(
            String question,
            List<PublicQueryRequest.HistoryMessage> history) {
        
        if (history == null || history.isEmpty()) {
            return question;
        }
        
        // קח את 2 ההודעות האחרונות של המשתמש (אם יש)
        StringBuilder contextBuilder = new StringBuilder();
        int userMessagesAdded = 0;
        
        for (int i = history.size() - 1; i >= 0 && userMessagesAdded < 2; i--) {
            PublicQueryRequest.HistoryMessage msg = history.get(i);
            if ("user".equals(msg.getRole())) {
                contextBuilder.insert(0, msg.getContent() + " ");
                userMessagesAdded++;
            }
        }
        
        // הוסף את השאלה הנוכחית
        contextBuilder.append(question);
        
        return contextBuilder.toString();
    }

    // ✅ בניית messages עם היסטוריה
    private List<ChatMessage> buildMessagesWithHistory(
            String question,
            List<RelevantDocument> relevantDocs,
            List<PublicQueryRequest.HistoryMessage> history) {
        
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

        // 2. הוספת היסטוריה (טקסט פשוט!)
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

        // 3. הקונטקסט מהמסמכים + השאלה החדשה
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

    // ✅ זיהוי שפה
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

    // ✅ בניית sources
    private List<QueryResponse.Source> buildSources(
            List<RelevantDocument> relevantDocs) {
        
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

    // ✅ חישוב confidence
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

    // ✅ קיצור טקסט
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    // ✅ תגובה "לא נמצאו תוצאות"
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