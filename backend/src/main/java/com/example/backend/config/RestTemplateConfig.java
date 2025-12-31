package com.example.backend.common.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * REST Template Configuration
 * 
 * Provides a configured RestTemplate bean for HTTP requests
 * Used by services that need to call external APIs (e.g., OpenAI)
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Create RestTemplate bean with timeouts
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))  // Connection timeout
                .setReadTimeout(Duration.ofSeconds(60))     // Read timeout (for OpenAI responses)
                .build();
    }
}