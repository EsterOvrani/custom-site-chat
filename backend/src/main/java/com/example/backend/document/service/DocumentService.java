// backend/src/main/java/com/example/backend/document/service/DocumentService.java
package com.example.backend.document.service;

import com.example.backend.document.dto.DocumentResponse;
import com.example.backend.document.mapper.DocumentMapper;
import com.example.backend.document.model.Document;
import com.example.backend.document.model.Document.ProcessingStatus;
import com.example.backend.document.model.Document.ProcessingStage;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.common.infrastructure.storage.S3Service;
import com.example.backend.common.infrastructure.vectordb.QdrantVectorService;
import com.example.backend.user.model.User;
import com.example.backend.user.repository.UserRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.io.ByteArrayInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentMapper documentMapper;
    private final S3Service s3Service;
    private final QdrantVectorService qdrantVectorService;
    private final EmbeddingModel embeddingModel;
    private final DocumentChunkingService chunkingService;

    // ==================== Main Processing Method ====================

    public DocumentResponse processDocument(MultipartFile file, User user) {
        log.info("🔵 processDocument() CALLED - preparing file for async processing");
        
        try {
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            long fileSize = file.getSize();
            
            log.info("✅ File read to memory: {} bytes", fileBytes.length);
            
            // ⭐ יצירת filePath מיד
            String filePath = generateFilePath(user, originalFilename);
            
            // ⭐ יצירת Document עם filePath תקין
            Document document = createDocumentEntity(originalFilename, fileSize, user, filePath, fileBytes);
            
            Integer maxOrder = documentRepository.getMaxDisplayOrderByUser(user);
            document.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);
            document.setProcessingStage(ProcessingStage.UPLOADING);
            document.setProcessingProgress(5);
            
            // ⭐ שמירה ב-DB - מיד!
            document = documentRepository.save(document);
            log.info("✅ Document entity created with ID: {} - RETURNING IMMEDIATELY", document.getId());
            
            // ⭐ המר ל-DTO כדי להחזיר לפרונטאנד
            DocumentResponse response = documentMapper.toResponse(document);
            
            // ⭐ קריאה לפונקציה אסינכרונית - זה ימשיך ברקע
            processDocumentAsync(
                document.getId(), 
                fileBytes, 
                originalFilename, 
                contentType, 
                fileSize, 
                filePath, 
                user
            );
            
            // ⭐ החזר את המסמך מיד!
            return response;
            
        } catch (IOException e) {
            log.error("❌ Failed to read file to memory", e);
            throw FileProcessingException.uploadFailed(file.getOriginalFilename());
        }
    }

    @Async
    public void processDocumentAsync(
            Long documentId,
            byte[] fileBytes,
            String originalFilename, 
            String contentType,
            long fileSize,
            String filePath,
            User user) {
        
        log.info("🔵 processDocumentAsync() STARTED for document ID: {}", documentId);

        try {
            // ==================== שלב 1: טעינת Document מה-DB ====================
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("מסמך", documentId));
            
            log.info("📍 Stage 1: Processing document {}", document.getId());

            // ==================== שלב 2: העלאה ל-S3 ====================
            log.info("📍 Stage 2: Uploading to S3");
            document.updateStage(ProcessingStage.UPLOADING, 10);
            documentRepository.save(document);
            
            s3Service.uploadFile(
                new ByteArrayInputStream(fileBytes),
                filePath,
                contentType,
                fileSize
            );
            
            document.updateStage(ProcessingStage.UPLOADING, 20);
            documentRepository.save(document);
            log.info("✅ File uploaded to S3");

            // ==================== שלב 3: ולידציה ====================
            log.info("📍 Stage 3: Validating file");
            validateFile(originalFilename, fileBytes);
            document.startProcessing();
            document.updateStage(ProcessingStage.EXTRACTING_TEXT, 25);
            documentRepository.save(document);

            // ==================== שלב 4: חילוץ טקסט ====================
            log.info("📍 Stage 4: Extracting text from PDF");
            DocumentParser parser = new ApachePdfBoxDocumentParser();
            
            dev.langchain4j.data.document.Document langchainDoc =
                parser.parse(new ByteArrayInputStream(fileBytes));
                
            String text = langchainDoc.text();
            int characterCount = text.length();
            document.setCharacterCount(characterCount);
            document.updateStage(ProcessingStage.EXTRACTING_TEXT, 40);
            documentRepository.save(document);
            
            log.info("✅ Extracted {} characters", characterCount);

            // ==================== שלב 5: חלוקה ל-chunks ====================
            log.info("📍 Stage 5: Splitting into chunks");
            document.updateStage(ProcessingStage.CREATING_CHUNKS, 50);
            documentRepository.save(document);
            
            List<TextSegment> segments = chunkingService.chunkDocument(
                text, 
                originalFilename,
                document.getId()
            );
            
            int chunkCount = segments.size();
            document.setChunkCount(chunkCount);
            document.updateStage(ProcessingStage.CREATING_CHUNKS, 60);
            documentRepository.save(document);
            
            log.info("✅ Split into {} chunks", chunkCount);

            // ==================== שלב 6: יצירת embeddings ושמירה ====================
            log.info("📍 Stage 6: Creating embeddings and storing in Qdrant");
            document.updateStage(ProcessingStage.CREATING_EMBEDDINGS, 65);
            documentRepository.save(document);
            
            String collectionName = user.getCollectionName();
            
            if (collectionName == null || collectionName.isEmpty()) {
                throw new ValidationException("user", "למשתמש אין קולקשן");
            }
            
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
                segment.metadata().put("user_id", user.getId().toString());
                
                // שמירה ב-Qdrant
                embeddingStore.add(embedding, segment);
                
                processed++;
                
                // עדכון התקדמות כל 10 chunks או בסיום
                if (processed % 10 == 0 || processed == segments.size()) {
                    int progress = baseProgress + 
                        ((maxProgress - baseProgress) * processed / segments.size());
                    
                    if (processed < segments.size()) {
                        document.updateStage(ProcessingStage.STORING, progress);
                    }
                    documentRepository.save(document);
                    
                    log.info("Progress: {}/{} chunks ({}%)", 
                        processed, segments.size(), progress);
                }
            }

            // ==================== שלב 7: סיום ====================
            log.info("📍 Stage 7: Finalizing");
            document.markAsCompleted(characterCount, chunkCount);
            documentRepository.save(document);
            
            log.info("✅ Document {} processed successfully", document.getId());

        } catch (Exception e) {
            log.error("🔴 EXCEPTION in processDocumentAsync()!", e);
            
            try {
                Document document = documentRepository.findById(documentId).orElse(null);
                if (document != null) {
                    document.markAsFailed(e.getMessage());
                    documentRepository.save(document);
                }
            } catch (Exception saveError) {
                log.error("Failed to save error state", saveError);
            }
            
            cleanupFile(filePath);
        }
    }

    // ==================== Helper Methods ====================

    private String generateFilePath(User user, String originalFilename) {
        return String.format("users/%d/documents/%s_%s",
            user.getId(),
            System.currentTimeMillis(),
            originalFilename
        );
    }

    private Document createDocumentEntity(
            String originalFilename, 
            long fileSize, 
            User user, 
            String filePath, 
            byte[] fileBytes) {
        
        Document document = new Document();
        document.setOriginalFileName(originalFilename);
        document.setFileType("pdf");
        document.setFileSize(fileSize);
        document.setFilePath(filePath);
        document.setProcessingStatus(ProcessingStatus.PENDING);
        document.setProcessingProgress(0);
        document.setProcessingStage(ProcessingStage.UPLOADING);
        document.setUser(user);
        document.setActive(true);

        try {
            String hash = calculateHash(fileBytes);
            document.setContentHash(hash);
        } catch (Exception e) {
            log.warn("Failed to calculate hash", e);
        }

        return document;
    }

    private void validateFile(String filename, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new ValidationException("file", "הקובץ ריק");
        }

        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw FileProcessingException.invalidFileType(filename, "PDF");
        }

        if (fileBytes.length > 50 * 1024 * 1024) {
            throw FileProcessingException.fileTooLarge(filename, 50L * 1024 * 1024);
        }
    }

    private String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (Exception e) {
            log.error("Failed to calculate hash", e);
            return null;
        }
    }

    private void cleanupFile(String filePath) {
        if (filePath != null) {
            try {
                s3Service.deleteFile(filePath);
                log.info("Cleaned up file: {}", filePath);
            } catch (Exception e) {
                log.warn("Failed to cleanup file: {}", filePath, e);
            }
        }
    }

    // ==================== Get Documents Methods ====================

    public List<DocumentResponse> getDocumentsByUser(User user) {
        List<Document> documents = documentRepository
            .findByUserAndActiveTrueOrderByDisplayOrderAsc(user);
        return documentMapper.toResponseList(documents);
    }

    public DocumentResponse getDocument(Long documentId, User user) {
        Document document = documentRepository.findByIdAndActiveTrue(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("מסמך", documentId));
        
        if (!document.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("מסמך", documentId);
        }

        return documentMapper.toResponse(document);
    }

    // ==================== Delete & Other Methods ====================
    
    private void deleteDocumentEmbeddings(Document document) {
        try {
            String collectionName = document.getUser().getCollectionName();
            if (collectionName != null) {
                qdrantVectorService.deleteDocumentEmbeddings(collectionName, document.getId());
            }
        } catch (Exception e) {
            log.error("Failed to delete embeddings", e);
        }
    }

    public void deleteDocument(Long documentId, User user) {
        Document document = documentRepository.findByIdAndActiveTrue(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("מסמך", documentId));
        
        if (!document.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("מסמך", documentId);
        }

        deleteDocumentEmbeddings(document);
        document.setActive(false);
        documentRepository.save(document);

        try {
            s3Service.deleteFile(document.getFilePath());
        } catch (Exception e) {
            log.warn("Failed to delete file from S3", e);
        }
    }

    @Transactional
    public int deleteAllDocumentsByUser(User user) {
        List<Document> documents = documentRepository
            .findByUserAndActiveTrueOrderByDisplayOrderAsc(user);

        if (documents.isEmpty()) {
            return 0;
        }

        for (Document doc : documents) {
            doc.setActive(false);
        }
        documentRepository.saveAll(documents);

        for (Document doc : documents) {
            try {
                s3Service.deleteFile(doc.getFilePath());
            } catch (Exception e) {
                log.warn("Failed to delete file", e);
            }
        }

        String collectionName = user.getCollectionName();
        if (collectionName != null) {
            try {
                qdrantVectorService.deleteCollection(collectionName);
                qdrantVectorService.createUserCollection(
                    user.getId().toString(), 
                    collectionName
                );
            } catch (Exception e) {
                log.error("Failed to reset collection", e);
            }
        }

        return documents.size();
    }

    public void reorderDocuments(User user, List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            throw new ValidationException("documentIds", "רשימת מסמכים ריקה");
        }

        for (int i = 0; i < documentIds.size(); i++) {
            Long docId = documentIds.get(i);
            Document doc = documentRepository.findByIdAndActiveTrue(docId)
                .orElseThrow(() -> new ResourceNotFoundException("מסמך", docId));

            if (!doc.getUser().getId().equals(user.getId())) {
                throw new UnauthorizedException("מסמך", docId);
            }

            doc.setDisplayOrder(i);
            documentRepository.save(doc);
        }
    }

    public DocumentStatistics getDocumentStatistics(User user) {
        long total = documentRepository.countByUserAndActiveTrue(user);
        long completed = documentRepository.countByUserAndProcessingStatusAndActiveTrue(
            user, ProcessingStatus.COMPLETED
        );
        long processing = documentRepository.countByUserAndProcessingStatusAndActiveTrue(
            user, ProcessingStatus.PROCESSING
        );
        long failed = documentRepository.countByUserAndProcessingStatusAndActiveTrue(
            user, ProcessingStatus.FAILED
        );

        List<Document> completedDocs = documentRepository
            .findByUserAndProcessingStatusAndActiveTrue(user, ProcessingStatus.COMPLETED);

        Long totalSize = completedDocs.stream()
            .map(Document::getFileSize)
            .reduce(0L, Long::sum);

        Integer totalChars = completedDocs.stream()
            .map(Document::getCharacterCount)
            .reduce(0, Integer::sum);

        Integer totalChunks = completedDocs.stream()
            .map(Document::getChunkCount)
            .reduce(0, Integer::sum);

        return DocumentStatistics.builder()
            .totalDocuments(total)
            .completedDocuments(completed)
            .processingDocuments(processing)
            .failedDocuments(failed)
            .totalFileSize(totalSize)
            .totalCharacters(totalChars)
            .totalChunks(totalChunks)
            .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class DocumentStatistics {
        private Long totalDocuments;
        private Long completedDocuments;
        private Long processingDocuments;
        private Long failedDocuments;
        private Long totalFileSize;
        private Integer totalCharacters;
        private Integer totalChunks;
    }
}