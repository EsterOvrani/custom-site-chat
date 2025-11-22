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
        if (user.hasCollection()) {
            return buildCollectionResponse(user);
        }
        return createNewCollection(user);
    }

    /**
     * יצירת קולקשן חדש למשתמש
     */
    private CollectionInfoResponse createNewCollection(User user) {
        log.info("Creating new collection for user: {}", user.getId());

        String collectionName = user.generateCollectionName();
        String secretKey = user.generateSecretKey();

        qdrantVectorService.createUserCollection(user.getId().toString(), collectionName);

        user.setCollectionName(collectionName);
        user.setCollectionSecretKey(secretKey);
        user.setCollectionCreatedAt(LocalDateTime.now());

        String embedCode = generateEmbedCode(secretKey);
        user.setEmbedCode(embedCode);

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