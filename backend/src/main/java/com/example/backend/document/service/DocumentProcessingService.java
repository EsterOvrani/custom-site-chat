package com.example.backend.document.service;

import com.example.backend.document.model.Document;
import com.example.backend.document.model.Document.ProcessingStage;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.common.infrastructure.storage.S3Service;
import com.example.backend.common.infrastructure.vectordb.QdrantVectorService;
import com.example.backend.common.infrastructure.document.DocumentChunkingService;
import com.example.backend.common.exception.*;

import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final S3Service s3Service;
    private final QdrantVectorService qdrantVectorService;
    private final EmbeddingModel embeddingModel;
    private final DocumentChunkingService chunkingService;

    /**
     * ⭐ עיבוד מסמך אסינכרוני - רץ ברקע!
     */
    @Async("documentProcessingExecutor")  // ⭐ שימוש ב-Thread Pool שהגדרנו
    public void processDocumentAsync(
            Long documentId,
            byte[] fileBytes,
            String originalFilename, 
            String contentType,
            long fileSize,
            String filePath,
            Long userId,
            String collectionName) {
        
        log.info("====================================================");
        log.info("🔵 [Thread: {}] Starting async processing for document ID: {}", 
            Thread.currentThread().getName(), documentId);
        log.info("====================================================");

        try {
            // ==================== טעינת Document מה-DB ====================
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("מסמך", documentId));
            
            log.info("📍 [{}] Stage 1: Document loaded from DB", documentId);

            // ==================== שלב 1: העלאה ל-S3 ====================
            log.info("📍 [{}] Stage 2: Uploading to S3", documentId);
            document.updateStage(ProcessingStage.UPLOADING, 10);
            documentRepository.save(document);
            log.info("✅ [{}] Progress updated: 10%", documentId);
            
            Thread.sleep(500); // ⭐ השהייה קטנה כדי לראות את העדכון
            
            s3Service.uploadFile(
                new ByteArrayInputStream(fileBytes),
                filePath,
                contentType,
                fileSize
            );
            
            document.updateStage(ProcessingStage.UPLOADING, 20);
            documentRepository.save(document);
            log.info("✅ [{}] File uploaded to S3 - Progress: 20%", documentId);

            Thread.sleep(500);

            // ==================== שלב 2: חילוץ טקסט ====================
            log.info("📍 [{}] Stage 3: Extracting text from PDF", documentId);
            document.updateStage(ProcessingStage.EXTRACTING_TEXT, 30);
            documentRepository.save(document);
            log.info("✅ [{}] Progress updated: 30%", documentId);
            
            Thread.sleep(500);
            
            DocumentParser parser = new ApachePdfBoxDocumentParser();
            dev.langchain4j.data.document.Document langchainDoc =
                parser.parse(new ByteArrayInputStream(fileBytes));
                
            String text = langchainDoc.text();
            int characterCount = text.length();
            document.setCharacterCount(characterCount);
            
            document.updateStage(ProcessingStage.EXTRACTING_TEXT, 45);
            documentRepository.save(document);
            log.info("✅ [{}] Extracted {} characters - Progress: 45%", 
                documentId, characterCount);

            Thread.sleep(500);

            // ==================== שלב 3: חלוקה ל-chunks ====================
            log.info("📍 [{}] Stage 4: Splitting into chunks", documentId);
            document.updateStage(ProcessingStage.CREATING_CHUNKS, 50);
            documentRepository.save(document);
            log.info("✅ [{}] Progress updated: 50%", documentId);
            
            Thread.sleep(500);
            
            List<TextSegment> segments = chunkingService.chunkDocument(
                text, 
                originalFilename,
                document.getId()
            );
            
            int chunkCount = segments.size();
            document.setChunkCount(chunkCount);
            
            document.updateStage(ProcessingStage.CREATING_CHUNKS, 60);
            documentRepository.save(document);
            log.info("✅ [{}] Split into {} chunks - Progress: 60%", 
                documentId, chunkCount);

            Thread.sleep(500);

            // ==================== שלב 4: יצירת embeddings ====================
            log.info("📍 [{}] Stage 5: Creating embeddings and storing", documentId);
            document.updateStage(ProcessingStage.CREATING_EMBEDDINGS, 65);
            documentRepository.save(document);
            log.info("✅ [{}] Progress updated: 65%", documentId);
            
            EmbeddingStore<TextSegment> embeddingStore = 
                qdrantVectorService.getEmbeddingStoreForCollection(collectionName);

            if (embeddingStore == null) {
                throw ExternalServiceException.vectorDbError(
                    "לא נמצא אחסון וקטורי עבור הקולקשן: " + collectionName
                );
            }

            // עיבוד embeddings עם התקדמות
            int processed = 0;
            int baseProgress = 65;
            int maxProgress = 95;
            
            for (TextSegment segment : segments) {
                // יצירת embedding
                Embedding embedding = embeddingModel.embed(segment).content();
                
                // הוספת metadata
                segment.metadata().put("document_id", document.getId().toString());
                segment.metadata().put("document_name", originalFilename);
                segment.metadata().put("chunk_index", String.valueOf(processed));
                segment.metadata().put("user_id", userId.toString());
                
                // שמירה ב-Qdrant
                embeddingStore.add(embedding, segment);
                
                processed++;
                
                // עדכון התקדמות כל 5 chunks
                if (processed % 5 == 0 || processed == segments.size()) {
                    int progress = baseProgress + 
                        ((maxProgress - baseProgress) * processed / segments.size());
                    
                    document.updateStage(ProcessingStage.STORING, progress);
                    documentRepository.save(document);
                    
                    log.info("✅ [{}] Progress: {}/{} chunks ({}%)", 
                        documentId, processed, segments.size(), progress);
                    
                    Thread.sleep(200); // השהייה קטנה לראות את העדכון
                }
            }

            // ==================== שלב 5: סיום ====================
            log.info("📍 [{}] Stage 6: Finalizing", documentId);
            document.markAsCompleted(characterCount, chunkCount);
            documentRepository.save(document);
            
            log.info("====================================================");
            log.info("✅ [{}] Document processed SUCCESSFULLY - 100%", documentId);
            log.info("====================================================");

        } catch (Exception e) {
            log.error("====================================================");
            log.error("❌ [{}] EXCEPTION in processDocumentAsync!", documentId, e);
            log.error("====================================================");
            
            try {
                Document document = documentRepository.findById(documentId).orElse(null);
                if (document != null) {
                    document.markAsFailed(e.getMessage());
                    documentRepository.save(document);
                }
            } catch (Exception saveError) {
                log.error("Failed to save error state", saveError);
            }
            
            try {
                s3Service.deleteFile(filePath);
            } catch (Exception cleanupError) {
                log.warn("Failed to cleanup file", cleanupError);
            }
        }
    }
}