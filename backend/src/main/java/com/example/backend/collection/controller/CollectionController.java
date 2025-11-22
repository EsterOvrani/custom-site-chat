package com.example.backend.collection.controller;

import com.example.backend.collection.service.CollectionService;
import com.example.backend.collection.dto.CollectionInfoResponse;
import com.example.backend.user.model.User;
import com.example.backend.common.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/collection")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CollectionController {

    private final CollectionService collectionService;

    /**
     * קבלת פרטי הקולקשן של המשתמש
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getCollectionInfo() {
        User currentUser = getCurrentUser();
        
        CollectionInfoResponse collectionInfo = 
            collectionService.getOrCreateUserCollection(currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", collectionInfo);

        return ResponseEntity.ok(response);
    }

    /**
     * יצירת מפתח חדש
     */
    @PostMapping("/regenerate-key")
    public ResponseEntity<Map<String, Object>> regenerateSecretKey() {
        User currentUser = getCurrentUser();
        
        CollectionInfoResponse collectionInfo = 
            collectionService.regenerateSecretKey(currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "מפתח חדש נוצר בהצלחה");
        response.put("data", collectionInfo);

        return ResponseEntity.ok(response);
    }

    /**
     * קבלת קוד הטמעה
     */
    @GetMapping("/embed-code")
    public ResponseEntity<Map<String, Object>> getEmbedCode() {
        User currentUser = getCurrentUser();
        
        CollectionInfoResponse collectionInfo = 
            collectionService.getOrCreateUserCollection(currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("embedCode", collectionInfo.getEmbedCode());

        return ResponseEntity.ok(response);
    }

    private User getCurrentUser() {
        Authentication authentication = 
            SecurityContextHolder.getContext().getAuthentication();

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