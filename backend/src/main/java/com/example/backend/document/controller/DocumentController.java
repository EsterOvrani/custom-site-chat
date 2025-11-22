package com.example.backend.document.controller;

import com.example.backend.document.dto.DocumentResponse;
import com.example.backend.document.service.DocumentService;
import com.example.backend.common.infrastructure.storage.S3Service;
import com.example.backend.user.model.User;
import com.example.backend.common.exception.ValidationException;
import com.example.backend.common.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing documents
 *
 * Endpoints:
 * - GET    /api/documents/my-documents    - Get all documents for current user
 * - GET    /api/documents/{id}            - Get document details
 * - GET    /api/documents/{id}/download   - Download document
 * - DELETE /api/documents/{id}            - Delete document
 * - PUT    /api/documents/reorder         - Reorder documents
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DocumentController {

    // ==================== Dependencies ====================
    
    private final DocumentService documentService;
    private final S3Service s3Service;

    // ==================== Get Documents ====================

    /**
     * Get all documents for the current user
     *
     * GET /api/documents/my-documents
     *
     * Response: List<DocumentResponse>
     */
    @GetMapping("/my-documents")
    public ResponseEntity<Map<String, Object>> getMyDocuments() {
        User currentUser = getCurrentUser();
        
        List<DocumentResponse> documents = 
            documentService.getDocumentsByUser(currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", documents);
        response.put("count", documents.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get specific document
     *
     * GET /api/documents/{id}
     *
     * Response: DocumentResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        
        DocumentResponse document = documentService.getDocument(id, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", document);

        return ResponseEntity.ok(response);
    }

    // ==================== Download Document ====================

    /**
     * Download original document
     *
     * GET /api/documents/{id}/download
     *
     * Response: PDF file
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable Long id) {
        User currentUser = getCurrentUser();

        // Get document details
        DocumentResponse document = documentService.getDocument(id, currentUser);

        // Download from S3
        InputStream fileStream = s3Service.downloadFile(document.getFilePath());

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData(
            "attachment",
            document.getOriginalFileName()
        );

        return ResponseEntity.ok()
            .headers(headers)
            .body(new InputStreamResource(fileStream));
    }

    /**
     * Get temporary download URL
     *
     * GET /api/documents/{id}/download-url
     *
     * Response: { "url": "..." }
     */
    @GetMapping("/{id}/download-url")
    public ResponseEntity<Map<String, Object>> getDownloadUrl(@PathVariable Long id) {
        User currentUser = getCurrentUser();

        // Get document details
        DocumentResponse document = documentService.getDocument(id, currentUser);

        // Create temporary URL (valid for 1 hour)
        String presignedUrl = s3Service.getPresignedUrl(
            document.getFilePath(),
            3600  // 1 hour
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("url", presignedUrl);
        response.put("expiresIn", 3600);

        return ResponseEntity.ok(response);
    }

    // ==================== Delete Document ====================

    /**
     * Delete document
     *
     * DELETE /api/documents/{id}
     *
     * Response: success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        documentService.deleteDocument(id, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "מסמך נמחק בהצלחה");

        return ResponseEntity.ok(response);
    }

    // ==================== Reorder Documents ====================

    /**
     * Reorder documents
     *
     * PUT /api/documents/reorder
     * Body: { "documentIds": [3, 1, 2] }
     *
     * Response: success message
     */
    @PutMapping("/reorder")
    public ResponseEntity<Map<String, Object>> reorderDocuments(
            @RequestBody Map<String, List<Long>> requestBody) {
        
        User currentUser = getCurrentUser();
        List<Long> documentIds = requestBody.get("documentIds");
        
        if (documentIds == null || documentIds.isEmpty()) {
            throw new ValidationException("documentIds", "רשימת מסמכים ריקה");
        }

        documentService.reorderDocuments(currentUser, documentIds);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "סדר המסמכים עודכן בהצלחה");

        return ResponseEntity.ok(response);
    }

    // ==================== Helper Methods ====================

    /**
     * Get currently authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("משתמש לא מחובר");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof User)) {
            throw new UnauthorizedException("משתמש לא תקין");
        }

        return (User) principal;
    }
}