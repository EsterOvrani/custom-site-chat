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

    // Get existing or create new collection
    public CollectionInfoResponse getOrCreateUserCollection(User user) {
        
        // Check: Does the user already have a collection
        if (user.hasCollection()) {
            // if yes:return the details.
            log.info("User {} already has collection: {}", 
                user.getId(), user.getCollectionName());
            return buildCollectionResponse(user);
        }
        
        // if not: create a new collection
        log.info("User {} doesn't have collection - creating new one", user.getId());
        return createNewCollection(user);
    }

    // Create Qdrant collection and save to user
    private CollectionInfoResponse createNewCollection(User user) {
        log.info("Creating new collection for user: {}", user.getId());

        // Create a unique collection name
        String collectionName = user.generateCollectionName();
        
        // Create a unique secret key
        String secretKey = user.generateSecretKey();
        
        // Creating the actual collection in Qdrant
        qdrantVectorService.createUserCollection(user.getId().toString(), collectionName);

        // Saving the details to the user (in the DB)
        user.setCollectionName(collectionName);
        user.setCollectionSecretKey(secretKey);
        user.setCollectionCreatedAt(LocalDateTime.now());

        // Create implementation code (done in the next step)
        String embedCode = generateEmbedCode(secretKey);
        user.setEmbedCode(embedCode);

        // Save user to DB
        userRepository.save(user);

        log.info("Collection created: {}", collectionName);
        return buildCollectionResponse(user);
    }

    // Generate new secret key and update embed code
    public CollectionInfoResponse regenerateSecretKey(User user) {
        log.info("Regenerating secret key for user: {}", user.getId());

        String newSecretKey = user.generateSecretKey();
        user.setCollectionSecretKey(newSecretKey);

        String embedCode = generateEmbedCode(newSecretKey);
        user.setEmbedCode(embedCode);

        userRepository.save(user);

        return buildCollectionResponse(user);
    }

    // Find user by secret key or throw
    public User validateSecretKey(String secretKey) {
        return userRepository.findByCollectionSecretKey(secretKey)
            .orElseThrow(() -> new UnauthorizedException("Invalid secret key"));
    }

    // Map user fields to response DTO
    private CollectionInfoResponse buildCollectionResponse(User user) {
        return CollectionInfoResponse.builder()
            .collectionName(user.getCollectionName())
            .secretKey(user.getCollectionSecretKey())
            .embedCode(user.getEmbedCode())
            .createdAt(user.getCollectionCreatedAt())
            .build();
    }

    // Generate JavaScript widget snippet
    private String generateEmbedCode(String secretKey) {
        return String.format(
            "<!-- Custom Site Chat Widget -->\n" +
            "<script>\n" +
            "  window.CHAT_WIDGET_SECRET_KEY = '%s';\n" +
            "  window.CHAT_WIDGET_API_URL = 'http://localhost:8080';\n" +
            "  \n" +
            "  // ⭐ התאמה אישית (אופציונלי)\n" +
            "  window.CHAT_WIDGET_TITLE = 'ENTER TITEL OF THE CHAT LIKE: COMPENY NAME'; \n" +
            "  window.CHAT_WIDGET_BOT_NAME = 'ENTER NAME OF THE BOT USER';\n" +
            "  window.CHAT_WIDGET_BOT_AVATAR = 'ENTER BOT PROFILE IMAGE OR NULL'; \n" +
            "  window.CHAT_WIDGET_USER_AVATAR = 'ENTER LINK OF USER PROFILE IMAGE OR NULL'; \n" +
            "</script>\n" +
            "<script src=\"http://localhost:3000/chat-widget.js\"></script>\n" +
            "<!-- End Chat Widget -->",
            secretKey
        );
    }
}