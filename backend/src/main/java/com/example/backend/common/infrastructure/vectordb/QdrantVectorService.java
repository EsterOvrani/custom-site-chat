package com.example.backend.common.infrastructure.vectordb;

import com.example.backend.common.exception.ExternalServiceException;
import com.example.backend.config.QdrantProperties;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QdrantVectorService {
    private final QdrantProperties qdrantProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    private String qdrantUrl;
    private final Map<String, EmbeddingStore<TextSegment>> collectionStoreMap = new ConcurrentHashMap<>();

    // Connect to Qdrant on startup
    @PostConstruct
    public void initialize() {
        try {
            qdrantUrl = String.format("http://%s:6333", qdrantProperties.getHost());

            log.info("Initializing Qdrant Vector service");
            log.info("Qdrant URL: {}", qdrantUrl);
            log.info("Qdrant Port (gRPC): {}", qdrantProperties.getPort());

            try {
                String healthUrl = qdrantUrl + "/health";
                ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Connected to Qdrant successfully");
                }
            } catch (Exception e) {
                log.warn("⚠️ Could not connect to Qdrant: {}", e.getMessage());
            }

            log.info("Qdrant Vector service initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Qdrant Vector service: {}", e.getMessage(), e);
            throw ExternalServiceException.vectorDbError("נכשל באתחול Qdrant: " + e.getMessage());
        }
    }

    // Create new Qdrant collection for user
    public String createUserCollection(String userId, String collectionName) {
        try {
            log.info("Creating collection for user {}: {}", userId, collectionName);

            createCollectionIfNotExists(collectionName);

            if (!waitForCollectionReady(collectionName, 30)) {
                throw ExternalServiceException.vectorDbError(
                    "פג זמן ההמתנה ליצירת קולקשן: " + collectionName
                );
            }

            EmbeddingStore<TextSegment> newStore = QdrantEmbeddingStore.builder()
                .host(qdrantProperties.getHost())
                .port(qdrantProperties.getPort())
                .collectionName(collectionName)
                .build();

            collectionStoreMap.put(collectionName, newStore);

            log.info("✅ User collection created: {}", collectionName);
            return collectionName;

        } catch (Exception e) {
            log.error("❌ Failed to create user collection", e);
            throw ExternalServiceException.vectorDbError(
                "נכשל ביצירת קולקשן: " + e.getMessage()
            );
        }
    }

    // Poll until collection is ready
    private boolean waitForCollectionReady(String collectionName, int maxWaitSeconds) {
        String checkUrl = qdrantUrl + "/collections/" + collectionName;

        int attempts = 0;
        int maxAttempts = maxWaitSeconds * 2;

        while (attempts < maxAttempts) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(checkUrl, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Collection '{}' is ready (attempt {}/{})",
                        collectionName, attempts + 1, maxAttempts);
                    return true;
                }

            } catch (Exception e) {
                log.debug("Collection not ready yet, waiting... (attempt {}/{})",
                    attempts + 1, maxAttempts);
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            attempts++;
        }

        log.error("❌ Collection '{}' not ready after {} seconds", collectionName, maxWaitSeconds);
        return false;
    }

    // Create collection with HNSW config
    private void createCollectionIfNotExists(String collectionName) {
        try {
            String getUrl = qdrantUrl + "/collections/" + collectionName;
            try {
                ResponseEntity<String> checkResponse = restTemplate.getForEntity(getUrl, String.class);
                if (checkResponse.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Collection '{}' already exists", collectionName);
                    return;
                }
            } catch (Exception e) {
                log.debug("Collection '{}' not found, creating new one", collectionName);
            }

            String createUrl = qdrantUrl + "/collections/" + collectionName;

            Map<String, Object> body = Map.of(
                    "vectors", Map.of(
                            "size", qdrantProperties.getDimension(),
                            "distance", qdrantProperties.getDistance()
                    ),
                    "hnsw_config", Map.of(
                            "m", qdrantProperties.getHnswM(),
                            "ef_construct", qdrantProperties.getHnswEfConstruct()
                    ),
                    "optimizers_config", Map.of(
                            "indexing_threshold", 10000
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    createUrl,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Collection '{}' created with HNSW optimization", collectionName);
            } else {
                log.error("❌ Failed to create collection '{}': {}", collectionName, response.getBody());
            }

        } catch (Exception e) {
            log.error("⚠️ Error managing collection '{}': {}", collectionName, e.getMessage(), e);
        }
    }

    // Delete embeddings by document ID
    public void deleteDocumentEmbeddings(String collectionName, Long documentId) {
        try {
            log.info("🗑️ Deleting embeddings for document {} from collection {}", 
                documentId, collectionName);

            String deleteUrl = qdrantUrl + "/collections/" + collectionName + "/points/delete";

            Map<String, Object> body = Map.of(
                "filter", Map.of(
                    "must", List.of(
                        Map.of(
                            "key", "document_id",
                            "match", Map.of("value", documentId.toString())
                        )
                    )
                )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                deleteUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Deleted embeddings for document: {}", documentId);
            } else {
                log.error("❌ Failed to delete embeddings: {}", response.getBody());
            }

        } catch (Exception e) {
            log.error("❌ Error deleting document embeddings", e);
            throw ExternalServiceException.vectorDbError(
                "נכשל במחיקת embeddings: " + e.getMessage()
            );
        }
    }

    // Get or create embedding store
    public EmbeddingStore<TextSegment> getEmbeddingStoreForCollection(String collectionName) {
        log.info("🔍 Looking for collection: {}", collectionName);
        log.info("📊 Available collections: {}", collectionStoreMap.keySet());

        EmbeddingStore<TextSegment> store = collectionStoreMap.get(collectionName);

        if (store == null) {
            log.warn("❌ Collection not in cache, trying to create...");
            createCollectionIfNotExists(collectionName);

            store = QdrantEmbeddingStore.builder()
                    .host(qdrantProperties.getHost())
                    .port(qdrantProperties.getPort())
                    .collectionName(collectionName)
                    .build();

            collectionStoreMap.put(collectionName, store);
        }

        return store;
    }

    // Remove from local cache
    public void removeCollectionFromCache(String collectionName) {
        collectionStoreMap.remove(collectionName);
        log.info("Collection removed from cache: {}", collectionName);
    }
    
    // Delete collection from Qdrant
    public void deleteCollection(String collectionName) {
        if (collectionName == null || collectionName.isEmpty()) {
            log.warn("⚠️ Cannot delete collection - name is null or empty");
            return;
        }

        try {
            log.info("🗑️ Deleting Qdrant collection: {}", collectionName);

            String deleteUrl = qdrantUrl + "/collections/" + collectionName;
            restTemplate.delete(deleteUrl);

            removeCollectionFromCache(collectionName);

            log.info("✅ Collection '{}' deleted successfully", collectionName);

        } catch (Exception e) {
            log.error("❌ Failed to delete collection: {}", collectionName, e);
            throw ExternalServiceException.vectorDbError("נכשל במחיקת קולקשן: " + collectionName);
        }
    }
}