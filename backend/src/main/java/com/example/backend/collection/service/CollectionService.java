package com.example.backend.collection.service;

import com.example.backend.collection.dto.CollectionInfoResponse;
import com.example.backend.user.model.User;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.common.infrastructure.vectordb.QdrantVectorService;
import com.example.backend.common.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private final UserRepository userRepository;
    private final QdrantVectorService qdrantVectorService;

    /**
     * יצירה או קבלת קולקשן של משתמש
     */
    public CollectionInfoResponse getOrCreateUserCollection(User user) {
        
        // ✅ בדיקה: האם למשתמש כבר יש קולקשן?
        if (user.hasCollection()) {
            // יש לו קולקשן - פשוט תחזיר את הפרטים
            log.info("User {} already has collection: {}", 
                user.getId(), user.getCollectionName());
            return buildCollectionResponse(user);
        }
        
        // אין לו קולקשן - צור חדש
        log.info("User {} doesn't have collection - creating new one", user.getId());
        return createNewCollection(user);
    }

    /**
     * יצירת קולקשן חדש למשתמש
     */
    private CollectionInfoResponse createNewCollection(User user) {
        log.info("Creating new collection for user: {}", user.getId());

        // שלב 1: צור שם קולקשן ייחודי
        String collectionName = user.generateCollectionName();
        // תוצאה: "user_123_a7b3f2e1"
        
        // שלב 2: צור secret key ייחודי
        String secretKey = user.generateSecretKey();
        // תוצאה: "sk_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"

        // ✅ שלב 3: יצירת הקולקשן בפועל ב-Qdrant
        qdrantVectorService.createUserCollection(user.getId().toString(), collectionName);

        // שלב 4: שמירת הפרטים במשתמש (בDB)
        user.setCollectionName(collectionName);
        user.setCollectionSecretKey(secretKey);
        user.setCollectionCreatedAt(LocalDateTime.now());

        // שלב 5: יצירת קוד הטמעה (נעשה בשלב הבא)
        String embedCode = generateEmbedCode(secretKey);
        user.setEmbedCode(embedCode);

        // שלב 6: שמירה ב-DB
        userRepository.save(user);

        log.info("Collection created: {}", collectionName);
        return buildCollectionResponse(user);
    }

    /**
     * יצירת מפתח חדש
     */
    public CollectionInfoResponse regenerateSecretKey(User user) {
        log.info("Regenerating secret key for user: {}", user.getId());

        String newSecretKey = user.generateSecretKey();
        user.setCollectionSecretKey(newSecretKey);

        String embedCode = generateEmbedCode(newSecretKey);
        user.setEmbedCode(embedCode);

        userRepository.save(user);

        return buildCollectionResponse(user);
    }

    /**
     * אימות secret key
     */
    public User validateSecretKey(String secretKey) {
        return userRepository.findByCollectionSecretKey(secretKey)
            .orElseThrow(() -> new UnauthorizedException("Invalid secret key"));
    }

    /**
     * בניית תגובה
     */
    private CollectionInfoResponse buildCollectionResponse(User user) {
        return CollectionInfoResponse.builder()
            .collectionName(user.getCollectionName())
            .secretKey(user.getCollectionSecretKey())
            .embedCode(user.getEmbedCode())
            .createdAt(user.getCollectionCreatedAt())
            .build();
    }

    /**
     * יצירת קוד הטמעה
     */
    private String generateEmbedCode(String secretKey) {
        return "<script>/* Chat Widget Code */</script>";
    }
}