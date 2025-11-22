package com.example.backend.document.service;

import com.example.backend.document.dto.DocumentResponse;
import com.example.backend.document.mapper.DocumentMapper;
import com.example.backend.document.model.Document;
import com.example.backend.document.model.Document.ProcessingStatus;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.common.infrastructure.storage.S3Service;
import com.example.backend.common.infrastructure.vectordb.QdrantVectorService;
import com.example.backend.user.model.User;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.common.infrastructure.document.DocumentChunkingService;
import com.example.backend.common.exception.ResourceNotFoundException;
import com.example.backend.common.exception.ValidationException;
import com.example.backend.common.exception.UnauthorizedException;
import com.example.backend.common.exception.FileProcessingException;
import com.example.backend.common.exception.ExternalServiceException;

// LangChain4j imports
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
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    // ==================== Main Processing Method ====================

    /**
     * Process document - entry point
     * Note: Not @Async here - delegates work to internal async function
     */
    public void processDocument(MultipartFile file, User user) {
        log.info("🔵 ========================================");
        log.info("🔵 processDocument() CALLED - preparing file for async processing");
        log.info("🔵 File: {}", file.getOriginalFilename());
        log.info("🔵 File size: {}", file.getSize());
        log.info("🔵 User ID: {}", user.getId());
        log.info("🔵 ========================================");
        
        try {
            // Step 1: Read file content to memory before async processing
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            long fileSize = file.getSize();
            
            log.info("✅ File read to memory: {} bytes", fileBytes.length);
            
            // Step 2: Pass data to async function
            processDocumentAsync(fileBytes, originalFilename, contentType, fileSize, user);
            
        } catch (IOException e) {
            log.error("❌ Failed to read file to memory: {}", file.getOriginalFilename(), e);
            throw FileProcessingException.uploadFailed(file.getOriginalFilename());
        }
    }

    /**
     * Actual document processing - runs in separate thread
     */
    @Async
    public void processDocumentAsync(
            byte[] fileBytes,
            String originalFilename, 
            String contentType,
            long fileSize,
            User user) {
        
        log.info("🔵 ========================================");
        log.info("🔵 processDocumentAsync() STARTED with LangChain4j!");
        log.info("🔵 File: {}", originalFilename);
        log.info("🔵 File bytes: {}", fileBytes.length);
        log.info("🔵 User ID: {}", user.getId());
        log.info("🔵 ========================================");

        Document document = null;
        String filePath = null;

        try {
            // Step 1: Upload to S3 - create InputStream from bytes
            log.info("📍 Step 1: Uploading to MinIO...");
            filePath = generateFilePath(user, originalFilename);
            
            s3Service.uploadFile(
                new ByteArrayInputStream(fileBytes),
                filePath,
                contentType,
                fileSize
            );
            log.info("✅ File uploaded to MinIO successfully");

            // Step 2: Create Document Entity
            log.info("📍 Step 2: Creating Document entity...");
            document = createDocumentEntity(originalFilename, fileSize, user, filePath, fileBytes);
            
            // Set display order
            Integer maxOrder = documentRepository.getMaxDisplayOrderByUser(user);
            document.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);
            
            document = documentRepository.save(document);
            log.info("✅ Document entity saved with ID: {} and size: {}", document.getId(), fileSize);

            // Step 3: Validate (check bytes, not MultipartFile)
            validateFile(originalFilename, fileBytes);
            document.startProcessing();
            document = documentRepository.save(document);

            // Step 4: Parse PDF using LangChain4j - create InputStream from bytes
            log.info("📍 Step 4: Parsing PDF with LangChain4j...");
            DocumentParser parser = new ApachePdfBoxDocumentParser();
            
            dev.langchain4j.data.document.Document langchainDoc =
                parser.parse(new ByteArrayInputStream(fileBytes));
                
            String text = langchainDoc.text();
            
            int characterCount = text.length();
            document.setCharacterCount(characterCount);
            log.info("✅ Extracted {} characters from PDF", characterCount);

            // Step 5: Split into chunks
            log.info("📍 Step 5: Splitting into chunks...");
            List<TextSegment> segments = chunkingService.chunkDocument(
                text, 
                originalFilename,
                document.getId()
            );
            
            int chunkCount = segments.size();
            document.setChunkCount(chunkCount);
            log.info("✅ Split into {} chunks", chunkCount);

            // Step 6: Store in Qdrant
            log.info("📍 Step 6: Storing in Qdrant...");
            String collectionName = user.getCollectionName();
            
            if (collectionName == null || collectionName.isEmpty()) {
                throw new ValidationException("user", "למשתמש אין קולקשן. אנא צור קולקשן תחילה.");
            }
            
            EmbeddingStore<TextSegment> embeddingStore = 
                qdrantVectorService.getEmbeddingStoreForCollection(collectionName);

            if (embeddingStore == null) {
                log.error("❌ No embedding store found for collection: {}", collectionName);
                throw ExternalServiceException.vectorDbError(
                    "לא נמצא אחסון וקטורי עבור הקולקשן: " + collectionName
                );
            }

            // Create embeddings and store
            int processed = 0;
            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                
                segment.metadata().put("document_id", document.getId().toString());
                segment.metadata().put("document_name", originalFilename);
                segment.metadata().put("chunk_index", String.valueOf(processed));
                segment.metadata().put("user_id", user.getId().toString());
                
                embeddingStore.add(embedding, segment);
                
                processed++;
                int progress = (processed * 100) / segments.size();
                document.setProcessingProgress(progress);
                documentRepository.save(document);
                
                log.debug("Processed chunk {}/{}", processed, segments.size());
            }

            // Step 7: Mark as Completed
            document.markAsCompleted(characterCount, chunkCount);
            documentRepository.save(document);
            
            log.info("✅ Document {} processed successfully", document.getId());

        } catch (FileProcessingException e) {
            log.error("🔴 File processing error: {}", e.getMessage());
            if (document != null) {
                document.markAsFailed(e.getMessage());
                documentRepository.save(document);
            }
            cleanupFile(filePath);
            throw e;
            
        } catch (Exception e) {
            log.error("🔴 EXCEPTION in processDocumentAsync()!", e);
            log.error("🔴 Exception type: {}", e.getClass().getName());
            log.error("🔴 Exception message: {}", e.getMessage());
            log.error("🔴 File name: {}", originalFilename);
            log.error("🔴 File size (reported): {}", fileSize);
            log.error("🔴 File bytes length: {}", fileBytes.length);
            
            if (document != null) {
                document.markAsFailed(e.getMessage());
                documentRepository.save(document);
            }
            
            cleanupFile(filePath);
            
            throw FileProcessingException.uploadFailed(originalFilename);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Generate file path in S3
     * Changed from chat-based to user-based
     */
    private String generateFilePath(User user, String originalFilename) {
        return String.format("users/%d/documents/%s_%s",
            user.getId(),
            System.currentTimeMillis(),
            originalFilename
        );
    }

    /**
     * Create document entity
     */
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
        document.setUser(user);
        document.setActive(true);

        try {
            String hash = calculateHash(fileBytes);
            document.setContentHash(hash);
        } catch (Exception e) {
            log.warn("Failed to calculate hash for file: {}", originalFilename);
        }

        return document;
    }

    /**
     * Validate file using bytes, not MultipartFile
     */
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
        
        log.info("✅ File validation passed: {} bytes", fileBytes.length);
    }

    /**
     * Calculate SHA-256 hash of file content
     */
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

    /**
     * Cleanup file from MinIO if processing failed
     */
    private void cleanupFile(String filePath) {
        if (filePath != null) {
            try {
                s3Service.deleteFile(filePath);
                log.info("Cleaned up file from MinIO: {}", filePath);
            } catch (Exception cleanupError) {
                log.warn("Failed to cleanup file from MinIO: {}", filePath, cleanupError);
            }
        }
    }

    // ==================== Get Documents Methods ====================

    /**
     * Get all documents for a user (ordered by display order)
     */
    public List<DocumentResponse> getDocumentsByUser(User user) {
        log.info("Getting documents for user: {}", user.getId());

        List<Document> documents = documentRepository
            .findByUserAndActiveTrueOrderByDisplayOrderAsc(user);

        return documentMapper.toResponseList(documents);
    }

    /**
     * Get a specific document
     */
    public DocumentResponse getDocument(Long documentId, User user) {
        log.info("Getting document: {}", documentId);

        Document document = documentRepository.findByIdAndActiveTrue(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("מסמך", documentId));
        
        if (!document.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("מסמך", documentId);
        }

        return documentMapper.toResponse(document);
    }

    /**
     * Get only processed documents
     */
    public List<DocumentResponse> getProcessedDocuments(User user) {
        log.info("Getting processed documents for user: {}", user.getId());

        List<Document> documents = documentRepository
            .findByUserAndProcessingStatusAndActiveTrue(user, ProcessingStatus.COMPLETED);

        return documentMapper.toResponseList(documents);
    }

    // ==================== Delete Document ====================

    /**
     * Delete a document (soft delete)
     */
    public void deleteDocument(Long documentId, User user) {
        log.info("Deleting document: {}", documentId);

        Document document = documentRepository.findByIdAndActiveTrue(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("מסמך", documentId));
        
        if (!document.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("מסמך", documentId);
        }

        // Soft delete
        document.setActive(false);
        documentRepository.save(document);

        // Delete from MinIO
        try {
            s3Service.deleteFile(document.getFilePath());
            log.info("Deleted file from MinIO: {}", document.getFilePath());
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO", e);
            throw ExternalServiceException.storageServiceError("נכשל במחיקת הקובץ מהאחסון");
        }

        // TODO: Delete embeddings from Qdrant for this specific document
        // This requires tracking which embeddings belong to which document
        log.warn("⚠️ Embeddings not deleted from Qdrant - implement if needed");
    }

    /**
     * Delete all documents for a user (used when deleting user/collection)
     */
    @Transactional
    public int deleteAllDocumentsByUser(User user) {
        try {
            log.info("🗑️ Deleting all documents for user: {}", user.getId());

            List<Document> documents = documentRepository
                .findByUserAndActiveTrueOrderByDisplayOrderAsc(user);

            if (documents.isEmpty()) {
                log.info("📂 No documents found to delete for user: {}", user.getId());
                return 0;
            }

            int count = documents.size();

            // Soft delete all
            for (Document doc : documents) {
                doc.setActive(false);
            }
            documentRepository.saveAll(documents);

            log.info("✅ Soft deleted {} document entities for user: {}", count, user.getId());
            return count;

        } catch (Exception e) {
            log.error("❌ Failed to delete documents for user: {}", user.getId(), e);
            throw new ResourceNotFoundException("נכשל במחיקת המסמכים");
        }
    }

    // ==================== Reorder Documents ====================

    /**
     * Reorder documents by user
     */
    public void reorderDocuments(User user, List<Long> documentIds) {
        log.info("Reordering documents for user: {}", user.getId());

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

        log.info("✅ Documents reordered successfully for user: {}", user.getId());
    }

    // ==================== Statistics ====================

    /**
     * Get document statistics for user
     */
    public DocumentStatistics getDocumentStatistics(User user) {
        log.info("Getting statistics for user: {}", user.getId());

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

        // Calculate total size and chunks
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

    // ==================== Statistics DTO ====================

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

        /**
         * Get formatted total size
         */
        public String getFormattedTotalSize() {
            if (totalFileSize == null || totalFileSize == 0) {
                return "0 B";
            }

            if (totalFileSize < 1024) {
                return totalFileSize + " B";
            } else if (totalFileSize < 1024 * 1024) {
                return String.format("%.2f KB", totalFileSize / 1024.0);
            } else if (totalFileSize < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", totalFileSize / (1024.0 * 1024.0));
            } else {
                return String.format("%.2f GB", totalFileSize / (1024.0 * 1024.0 * 1024.0));
            }
        }

        /**
         * Get completion percentage
         */
        public double getCompletionPercentage() {
            if (totalDocuments == null || totalDocuments == 0) {
                return 0.0;
            }
            return (completedDocuments * 100.0) / totalDocuments;
        }

        /**
         * Check if any documents are processing
         */
        public boolean isProcessing() {
            return processingDocuments != null && processingDocuments > 0;
        }

        /**
         * Check if any documents failed
         */
        public boolean hasFailed() {
            return failedDocuments != null && failedDocuments > 0;
        }
    }
}