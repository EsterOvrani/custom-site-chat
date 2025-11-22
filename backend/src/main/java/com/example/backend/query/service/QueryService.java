package com.example.backend.query.service;

import com.example.backend.collection.service.CollectionService;
import com.example.backend.query.dto.PublicQueryRequest;
import com.example.backend.query.dto.AnswerResponse; 
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    /**
     * שאילת שאלה ציבורית (מהאתר המוטמע)
     */
    public AnswerResponse askPublicQuestion(PublicQueryRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. אימות secret key וקבלת משתמש
            User user = collectionService.validateSecretKey(request.getSecretKey());
            
            log.info("Public query from user: {} (session: {})", 
                user.getId(), request.getSessionId());

            // 2. חיפוש מסמכים רלוונטיים
            List<RelevantDocument> relevantDocs = searchRelevantDocuments(
                user.getCollectionName(),
                request.getQuestion()
            );

            if (relevantDocs.isEmpty()) {
                return createNoResultsResponse();
            }

            // 3. בניית ההקשר ושאילת AI
            List<dev.langchain4j.data.message.ChatMessage> messages = 
                buildChatMessages(request.getQuestion(), relevantDocs);

            Response<AiMessage> response = chatModel.generate(messages);
            String answer = response.content().text();

            // 4. חישוב מטריקות
            long responseTime = System.currentTimeMillis() - startTime;
            Double confidence = calculateConfidence(relevantDocs);

            OpenAiTokenizer tokenizer = new OpenAiTokenizer("gpt-4");
            int tokensUsed = tokenizer.estimateTokenCountInMessage(response.content());

            List<AnswerResponse.Source> sources = buildSources(relevantDocs);

            // 5. החזרת תשובה (ללא שמירה ב-DB!)
            return AnswerResponse.builder()
                .answer(answer)
                .success(true)
                .confidence(confidence)
                .sources(sources)
                .tokensUsed(tokensUsed)
                .responseTimeMs(responseTime)
                .timestamp(LocalDateTime.now())
                .build();

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to answer public question", e);
            return createErrorResponse(e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    private List<RelevantDocument> searchRelevantDocuments(
            String collectionName, 
            String question) {
        
        try {
            EmbeddingStore<TextSegment> embeddingStore = 
                qdrantVectorService.getEmbeddingStoreForCollection(collectionName);

            if (embeddingStore == null) {
                log.error("❌ No embedding store for collection: {}", collectionName);
                return new ArrayList<>();
            }

            Embedding queryEmbedding = embeddingModel.embed(question).content();

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(MAX_RELEVANT_CHUNKS)
                .minScore(0.5)
                .build();

            EmbeddingSearchResult<TextSegment> searchResult = 
                embeddingStore.search(searchRequest);

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

    private List<dev.langchain4j.data.message.ChatMessage> buildChatMessages(
            String question,
            List<RelevantDocument> relevantDocs) {

        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();

        messages.add(SystemMessage.from(
            "You are a helpful AI assistant. " +
            "Answer in the SAME LANGUAGE as the question. " +
            "Base your answer only on the provided documents."
        ));

        StringBuilder context = new StringBuilder();
        context.append("Relevant information:\n\n");
        
        for (int i = 0; i < relevantDocs.size(); i++) {
            RelevantDocument doc = relevantDocs.get(i);
            context.append(String.format("[Document %d - %s]:\n%s\n\n",
                i + 1,
                doc.getDocumentName(),
                doc.getText()
            ));
        }

        messages.add(UserMessage.from(
            context.toString() + "\nQuestion: " + question
        ));

        return messages;
    }

    private List<AnswerResponse.Source> buildSources(
            List<RelevantDocument> relevantDocs) {
        
        List<AnswerResponse.Source> sources = new ArrayList<>();

        for (int i = 0; i < relevantDocs.size(); i++) {
            RelevantDocument doc = relevantDocs.get(i);

            AnswerResponse.Source source = AnswerResponse.Source.builder()
                .documentName(doc.getDocumentName())
                .excerpt(truncateText(doc.getText(), 200))
                .relevanceScore(doc.getScore())
                .isPrimary(i == 0)
                .build();

            sources.add(source);
        }

        return sources;
    }

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

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private AnswerResponse createNoResultsResponse() {
        return AnswerResponse.builder()
            .answer("Sorry, I couldn't find relevant information.")
            .success(true)
            .confidence(0.0)
            .timestamp(LocalDateTime.now())
            .build();
    }

    private AnswerResponse createErrorResponse(String errorMessage) {
        return AnswerResponse.builder()
            .success(false)
            .errorMessage(errorMessage)
            .timestamp(LocalDateTime.now())
            .build();
    }

    @lombok.Data
    private static class RelevantDocument {
        private String text;
        private Double score;
        private String documentName;
    }
}