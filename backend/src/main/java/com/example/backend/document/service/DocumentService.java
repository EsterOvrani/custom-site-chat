package com.example.backend.document.service;

import com.example.backend.document.dto.DocumentResponse;
import com.example.backend.document.mapper.DocumentMapper;
import com.example.backend.document.dto.DuplicateCheckResponse;
import com.example.backend.document.event.DocumentCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import java.util.Optional;
import com.example.backend.document.model.Document;
import com.example.backend.document.model.Document.ProcessingStatus;
import com.example.backend.document.model.Document.ProcessingStage;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.common.infrastructure.storage.S3Service;
import com.example.backend.common.infrastructure.vectordb.QdrantVectorService;
import com.example.backend.user.model.User;
import com.example.backend.common.exception.*;
import org.springframework.transaction.annotation.Propagation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final S3Service s3Service;
    private final QdrantVectorService qdrantVectorService;
    private final DocumentProcessingService documentProcessingService;
    private final ApplicationEventPublisher eventPublisher; // ✅ NEW: For event publishing
    

    /**
     * Check if a file with the same name exists
     */
    public DuplicateCheckResponse checkDuplicate(String fileName, User user) {
        Optional<Document> existingDoc = documentRepository
            .findByUserAndFileName(user, fileName);
        
        if (existingDoc.isPresent()) {
            String suggestedName = generateUniqueName(fileName, user);
            
            return DuplicateCheckResponse.builder()
                .exists(true)
                .existingDocumentId(existingDoc.get().getId())
                .fileName(fileName)
                .suggestedName(suggestedName)
                .build();
        }
        
        return DuplicateCheckResponse.builder()
            .exists(false)
            .fileName(fileName)
            .build();
    }
    
    /**
     * Generate unique file name (Windows-style)
     * file.pdf -> file (1).pdf -> file (2).pdf
     */
    private String generateUniqueName(String fileName, User user) {
        String baseName;
        String extension = "";
        
        // Split file name and extension
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            baseName = fileName.substring(0, lastDot);
            extension = fileName.substring(lastDot);
        } else {
            baseName = fileName;
        }
        
        // Find available number
        int counter = 1;
        String newName;
        
        do {
            newName = baseName + " (" + counter + ")" + extension;
            counter++;
        } while (documentRepository.findByUserAndFileName(user, newName).isPresent());
        
        return newName;
    }

    /**
     * Process document with replacement
     * This handles the "Replace" option from the duplicate dialog
     */
    public DocumentResponse processDocumentWithReplacement(
        MultipartFile file, 
        User user, 
        Long replaceDocumentId) {
        log.info("====================================================");
        log.info("🔄 REPLACEMENT MODE STARTED");
        log.info("Old Document ID: {}", replaceDocumentId);
        log.info("New File: {}", file.getOriginalFilename());
        log.info("User ID: {}", user.getId());
        log.info("====================================================");
        
        // Step 1: Validate - ensure old document exists and belongs to user
        log.info("Step 1: Validating old document...");
        Document oldDocument = documentRepository.findByIdAndActiveTrue(replaceDocumentId)
            .orElseThrow(() -> new ResourceNotFoundException("מסמך", replaceDocumentId));
        
        log.info("✅ Old document found: ID={}, Name={}", 
            oldDocument.getId(), oldDocument.getOriginalFileName());
        
        if (!oldDocument.getUser().getId().equals(user.getId())) {
            log.error("❌ Unauthorized access attempt! Document user: {}, Request user: {}", 
                oldDocument.getUser().getId(), user.getId());
            throw new UnauthorizedException("מסמך", replaceDocumentId);
        }
        
        log.info("✅ Ownership validated");
        
        // Step 2: Soft Delete immediately
        log.info("Step 2: Soft deleting old document...");
        oldDocument.setActive(false);
        documentRepository.saveAndFlush(oldDocument);
        log.info("✅ Old document marked as inactive (ID: {}) - FLUSHED", replaceDocumentId);
        
        // Step 3: Upload the new document
        log.info("Step 3: Uploading new document...");
        DocumentResponse newDocument;
        try {
            newDocument = processDocument(file, user);
            log.info("✅ New document uploaded successfully (ID: {})", newDocument.getId());
            log.info("New document status: {}, progress: {}, stage: {}", 
                newDocument.getProcessingStatus(), 
                newDocument.getProcessingProgress(),
                newDocument.getProcessingStage());
        } catch (Exception e) {
            log.error("❌ Failed to upload new document", e);
            throw new FileProcessingException(
                "כישלון בהעלאת הקובץ החדש. נסה שוב."
            );
        }
        
        // Step 4: Schedule full deletion of old document (in background)
        log.info("Step 4: Scheduling deletion of old document (async)...");
        scheduleOldDocumentDeletion(replaceDocumentId, user);
        log.info("✅ Deletion scheduled");
        
        log.info("====================================================");
        log.info("🎉 REPLACEMENT MODE COMPLETED - Returning new document");
        log.info("====================================================");
        
        return newDocument;
    }
    
    /**
     * Schedule full deletion of old document (async, in background)
     * This deletes: embeddings from Qdrant, file from S3, record from DB
     */
    @Async
    private void scheduleOldDocumentDeletion(Long documentId, User user) {
        try {
            // Wait 2 seconds to ensure new document upload has started
            Thread.sleep(2000);
            
            log.info("🗑️ Starting full deletion of old document: {}", documentId);
            
            // Reuse existing delete method!
            deleteDocument(documentId, user);
            
            log.info("✅ Old document deleted successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Deletion interrupted for document: {}", documentId, e);
        } catch (Exception e) {
            log.error("❌ Failed to delete old document: {}", documentId, e);
            // Don't throw - we don't want to crash the system
            // Old document stays with active=false, can be cleaned up later
        }
    }

    /**
     * ✅ FIXED: Save document and publish event for async processing
     * The event will be handled AFTER the transaction commits, ensuring the document is visible
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DocumentResponse processDocument(MultipartFile file, User user) {
        log.info("====================================================");
        log.info("🔵 processDocument() CALLED");
        log.info("File: {}", file.getOriginalFilename());
        log.info("User ID: {}", user.getId());
        log.info("====================================================");
        
        try {
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            long fileSize = file.getSize();
            
            log.info("✅ File read to memory: {} bytes", fileBytes.length);
            
            // create filePath
            String filePath = generateFilePath(user, originalFilename);
            log.info("Generated file path: {}", filePath);
            
            // create Document
            log.info("Creating document entity...");
            Document document = createDocumentEntity(originalFilename, fileSize, user, filePath, fileBytes);
            
            Integer maxOrder = documentRepository.getMaxDisplayOrderByUser(user);
            document.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);
            document.setProcessingStatus(ProcessingStatus.PENDING);
            document.setProcessingStage(ProcessingStage.UPLOADING);
            document.setProcessingProgress(5);
            
            log.info("Document entity created - display order: {}", document.getDisplayOrder());
            
            // save document to DB and FLUSH immediately!
            log.info("Saving document to DB...");
            document = documentRepository.saveAndFlush(document);
            log.info("====================================================");
            log.info("✅ Document saved with ID: {} - FLUSHED TO DB", document.getId());
            log.info("Status: {}, Progress: {}, Stage: {}", 
                document.getProcessingStatus(), 
                document.getProcessingProgress(), 
                document.getProcessingStage());
            log.info("====================================================");
            
            // convert to DTO object
            DocumentResponse response = documentMapper.toResponse(document);
            
            // ✅ FIX: Publish event instead of calling async directly
            // This ensures the event is processed AFTER the transaction commits
            log.info("📢 Publishing DocumentCreatedEvent for ID: {}", document.getId());
            eventPublisher.publishEvent(new DocumentCreatedEvent(
                document.getId(), 
                fileBytes, 
                originalFilename, 
                contentType, 
                fileSize, 
                filePath,
                user.getId(),
                user.getCollectionName()
            ));
            log.info("✅ Event published - will process after transaction commit");
            
            // Returns the response immediately 
            return response;
            
        } catch (IOException e) {
            log.error("❌ Failed to read file to memory", e);
            throw FileProcessingException.uploadFailed(file.getOriginalFilename());
        }
    }

    /**
     * Create S3 path for document
     */
    private String generateFilePath(User user, String originalFilename) {
        return String.format("users/%d/documents/%s_%s",
            user.getId(),
            System.currentTimeMillis(),
            originalFilename
        );
    }

    /**
     * Build document entity with metadata
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

    /**
     * Calculate SHA-256 hash of file
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
     * Get all active documents for user
     */
    public List<DocumentResponse> getDocumentsByUser(User user) {
        List<Document> documents = documentRepository
            .findByUserAndActiveTrueOrderByDisplayOrderAsc(user);
        return documentMapper.toResponseList(documents);
    }

    /**
     * Get document with ownership check
     */
    public DocumentResponse getDocument(Long documentId, User user) {
        Document document = documentRepository.findByIdAndActiveTrue(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("מסמך", documentId));
        
        if (!document.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("מסמך", documentId);
        }

        return documentMapper.toResponse(document);
    }

    /**
     * Delete document with S3 and Qdrant cleanup
     */
    public void deleteDocument(Long documentId, User user) {
        Document document = documentRepository.findByIdAndActiveTrue(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("מסמך", documentId));
        
        if (!document.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("מסמך", documentId);
        }

        // Delete embeddings from Qdrant
        try {
            String collectionName = user.getCollectionName();
            if (collectionName != null) {
                qdrantVectorService.deleteDocumentEmbeddings(collectionName, document.getId());
            }
        } catch (Exception e) {
            log.error("Failed to delete embeddings", e);
        }

        // Soft delete in DB
        document.setActive(false);
        documentRepository.save(document);

        // Delete physical file from S3
        try {
            s3Service.deleteFile(document.getFilePath());
        } catch (Exception e) {
            log.warn("Failed to delete file from S3", e);
        }
    }

    /**
     * Delete all documents and reset collection
     */
    @Transactional
    public int deleteAllDocumentsByUser(User user) {
        List<Document> documents = documentRepository
            .findByUserAndActiveTrueOrderByDisplayOrderAsc(user);

        if (documents.isEmpty()) {
            return 0;
        }

        // Soft delete all documents
        for (Document doc : documents) {
            doc.setActive(false);
        }
        documentRepository.saveAll(documents);

        // Delete physical files from S3
        for (Document doc : documents) {
            try {
                s3Service.deleteFile(doc.getFilePath());
            } catch (Exception e) {
                log.warn("Failed to delete file", e);
            }
        }

        // Reset Qdrant collection
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

    /**
     * Update display order with validation
     */
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
}