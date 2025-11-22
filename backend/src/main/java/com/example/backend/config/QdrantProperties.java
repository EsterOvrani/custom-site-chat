package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data 
@Component
@ConfigurationProperties(prefix = "qdrant")
public class QdrantProperties {

    private String host = "localhost";
    private int port = 6334;
    private String collectionName = "custom_site_chat";
    private boolean useTls = false;
    private String apiKey;

    private int dimension = 3072;
    private String distance = "Cosine";

    private int defaultMaxResults = 5;
    private double defaultMinScore = 0.5;

    private int hnswM = 16;
    private int hnswEfConstruct = 200;
    private int hnswEf = 128;
}