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
import com.example.backend.common.exception.*;

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
    
    // ⭐ Inject את ה-Service החדש
    private final DocumentProcessingService documentProcessingService;

    public DocumentResponse processDocument(MultipartFile file, User user) {
        log.info("🔵 processDocument() CALLED - preparing file for async processing");
        
        try {
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            long fileSize = file.getSize();
            
            log.info("✅ File read to memory: {} bytes", fileBytes.length);
            
            // יצירת filePath
            String filePath = generateFilePath(user, originalFilename);
            
            // יצירת Document
            Document document = createDocumentEntity(originalFilename, fileSize, user, filePath, fileBytes);
            
            Integer maxOrder = documentRepository.getMaxDisplayOrderByUser(user);
            document.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);
            document.setProcessingStatus(ProcessingStatus.PENDING);
            document.setProcessingStage(ProcessingStage.UPLOADING);
            document.setProcessingProgress(5);
            
            // שמירה ב-DB
            document = documentRepository.save(document);
            log.info("✅ Document entity created with ID: {} - RETURNING IMMEDIATELY", document.getId());
            
            // המר ל-DTO
            DocumentResponse response = documentMapper.toResponse(document);
            
            // ⭐ קריאה ל-Service החדש (לא self-invocation!)
            documentProcessingService.processDocumentAsync(
                document.getId(), 
                fileBytes, 
                originalFilename, 
                contentType, 
                fileSize, 
                filePath,
                user.getId(),
                user.getCollectionName()
            );
            
            // החזר מיד
            return response;
            
        } catch (IOException e) {
            log.error("❌ Failed to read file to memory", e);
            throw FileProcessingException.uploadFailed(file.getOriginalFilename());
        }
    }

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

    // ... שאר הפונקציות נשארות אותו דבר (getDocument, deleteDocument, וכו')
    
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

    public void deleteDocument(Long documentId, User user) {
        Document document = documentRepository.findByIdAndActiveTrue(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("מסמך", documentId));
        
        if (!document.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("מסמך", documentId);
        }

        try {
            String collectionName = user.getCollectionName();
            if (collectionName != null) {
                qdrantVectorService.deleteDocumentEmbeddings(collectionName, document.getId());
            }
        } catch (Exception e) {
            log.error("Failed to delete embeddings", e);
        }

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
}
