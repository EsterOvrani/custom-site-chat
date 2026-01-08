package com.example.backend.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(QdrantProperties.class)
public class QdrantConfig {

    private final QdrantProperties qdrantProperties;

    @Value("${OPENAI_API_KEY}")
    private String openaiApiKey;

    /**
     * create EmbeddingModel of OpenAI
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Creating OpenAI embedding model with text-embedding-3-large");
        return OpenAiEmbeddingModel.builder()
                .apiKey(openaiApiKey)
                .modelName("text-embedding-3-large")
                .build();
    }

    /**
     * create OpenAiChatModel with GPT-4o
     */
    @Bean
    public OpenAiChatModel openAiChatModel() {
        log.info("🚀 Creating OpenAI Chat model with GPT-4o");
        return OpenAiChatModel.builder()
                .apiKey(openaiApiKey)
                .modelName("gpt-4o")
                .temperature(0.7)
                .build();
    }
}