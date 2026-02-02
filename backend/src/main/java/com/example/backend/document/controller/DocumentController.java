package com.example.backend.document.controller;

import com.example.backend.document.dto.DocumentResponse;
import com.example.backend.document.dto.DuplicateCheckResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;
    private final S3Service s3Service;


    /**
     * Check if a file with the same name already exists
     * Called by frontend before upload to detect duplicates
     */
    @PostMapping("/check-duplicate")
    public ResponseEntity<Map<String, Object>> checkDuplicate(
            @RequestParam("fileName") String fileName) {
        
        User currentUser = getCurrentUser();
        
        // Check for duplicate
        DuplicateCheckResponse result = documentService.checkDuplicate(fileName, currentUser);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Upload document with optional replacement
     * If replaceDocumentId is provided, it replaces the old document
     * Otherwise, it's a normal upload
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replaceDocumentId", required = false) Long replaceDocumentId) {
        
        User currentUser = getCurrentUser();
        
        DocumentResponse document;
        String message;
        
        // Decision: is this a replacement or normal upload?
        if (replaceDocumentId != null) {
            // Replace existing document
            document = documentService.processDocumentWithReplacement(
                file, 
                currentUser, 
                replaceDocumentId
            );
            message = "המסמך הוחלף ומעובד ברקע";
        } else {
            // Normal upload
            document = documentService.processDocument(file, currentUser);
            message = "המסמך הועלה ומעובד ברקע";
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("document", document); 

        return ResponseEntity.ok(response);
    }

   // Get all documents for current user
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

    // Get single document by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        
        DocumentResponse document = documentService.getDocument(id, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", document);

        return ResponseEntity.ok(response);
    }

    // Download document as attachment
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable Long id) {
        User currentUser = getCurrentUser();

        DocumentResponse document = documentService.getDocument(id, currentUser);

        InputStream fileStream = s3Service.downloadFile(document.getFilePath());

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

    // View document inline in browser
    @GetMapping("/{id}/view")
    public ResponseEntity<?> viewDocument(@PathVariable Long id) {
        User currentUser = getCurrentUser();

        DocumentResponse document = documentService.getDocument(id, currentUser);

        InputStream fileStream = s3Service.downloadFile(document.getFilePath());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData(
            "inline",
            document.getOriginalFileName()
        );

        return ResponseEntity.ok()
            .headers(headers)
            .body(new InputStreamResource(fileStream));
    }

    // Get presigned S3 URL
    @GetMapping("/{id}/download-url")
    public ResponseEntity<Map<String, Object>> getDownloadUrl(@PathVariable Long id) {
        User currentUser = getCurrentUser();

        DocumentResponse document = documentService.getDocument(id, currentUser);

        String presignedUrl = s3Service.getPresignedUrl(
            document.getFilePath(),
            3600
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("url", presignedUrl);
        response.put("expiresIn", 3600);

        return ResponseEntity.ok(response);
    }

    // Soft delete single document
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        documentService.deleteDocument(id, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "מסמך נמחק בהצלחה");

        return ResponseEntity.ok(response);
    }

    // Soft delete all user documents
    @DeleteMapping("/delete-all")
    public ResponseEntity<Map<String, Object>> deleteAllDocuments() {
        User currentUser = getCurrentUser();
        
        int deletedCount = documentService.deleteAllDocumentsByUser(currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "כל המסמכים נמחקו בהצלחה");
        response.put("deletedCount", deletedCount);

        return ResponseEntity.ok(response);
    }

    // Update display order of documents
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

    // Extract user from security context
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