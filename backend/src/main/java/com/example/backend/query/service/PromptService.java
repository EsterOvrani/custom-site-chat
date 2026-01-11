package com.example.backend.query.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PromptService {

    private final Map<String, String> promptCache = new HashMap<>();
    
    // Prompt file names
    private static final String QUERY_REWRITE = "query-rewrite.txt";
    private static final String SYSTEM_MESSAGE = "system-message.txt";
    private static final String NO_RESULTS_HEBREW = "no-results-hebrew.txt";
    private static final String NO_RESULTS_ENGLISH = "no-results-english.txt";
    private static final String ANALYTICS_FILTER = "analytics-filter.txt";
    private static final String ANALYTICS_ANALYSIS = "analytics-analysis.txt";
    
    @PostConstruct
    public void init() {
        log.info("🔄 Loading prompts from resources/prompts/");
        
        loadPrompt(QUERY_REWRITE);
        loadPrompt(SYSTEM_MESSAGE);
        loadPrompt(NO_RESULTS_HEBREW);
        loadPrompt(NO_RESULTS_ENGLISH);
        loadPrompt(ANALYTICS_FILTER);
        loadPrompt(ANALYTICS_ANALYSIS);
        
        log.info("✅ Loaded {} prompts successfully", promptCache.size());
    }
    
    private void loadPrompt(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/" + fileName);
            String content = new String(
                resource.getInputStream().readAllBytes(), 
                StandardCharsets.UTF_8
            );
            promptCache.put(fileName, content);
            log.info("✅ Loaded prompt: {}", fileName);
        } catch (IOException e) {
            log.error("❌ Failed to load prompt: {}", fileName, e);
            throw new RuntimeException("Failed to load prompt: " + fileName, e);
        }
    }
    
    /**
     * Get query rewrite prompt for the detected language
     */
    public String getQueryRewritePrompt(String language, String context, String question) {
        String template = promptCache.get(QUERY_REWRITE);
        
        if (template == null) {
            log.error("❌ Prompt template not found");
            throw new RuntimeException("Prompt template not found");
        }
        
        String languageName = language.equals("he") ? "HEBREW" : "ENGLISH";
        return String.format(template, languageName, context, question, languageName);
    }
    
    /**
     * Get system message prompt
     */
    public String getSystemMessage(String language) {
        String template = promptCache.get(SYSTEM_MESSAGE);
        
        if (template == null) {
            log.error("❌ System message template not found");
            throw new RuntimeException("System message template not found");
        }
        
        return String.format(template, language.toUpperCase());
    }
    
    /**
     * Get no results message
     */
    public String getNoResultsMessage(String language) {
        String key = language.equals("he") 
            ? NO_RESULTS_HEBREW 
            : NO_RESULTS_ENGLISH;
        
        String message = promptCache.get(key);
        
        if (message == null) {
            log.error("❌ No results message not found for language: {}", language);
            return "Sorry, I don't have this information.";
        }
        
        return message;
    }
    
    /**
     * Get analytics filter prompt (includes system message in first line)
     */
    public String getAnalyticsFilterPrompt(String siteCategory, String questionsText) {
        String template = promptCache.get(ANALYTICS_FILTER);
        
        if (template == null) {
            log.error("❌ Analytics filter prompt not found");
            throw new RuntimeException("Analytics filter prompt not found");
        }
        
        return String.format(template, siteCategory, questionsText);
    }
    
    /**
     * Get analytics analysis prompt (includes system message in first line)
     */
    public String getAnalyticsAnalysisPrompt(String questionsText) {
        String template = promptCache.get(ANALYTICS_ANALYSIS);
        
        if (template == null) {
            log.error("❌ Analytics analysis prompt not found");
            throw new RuntimeException("Analytics analysis prompt not found");
        }
        
        return String.format(template, questionsText);
    }
    
    /**
     * Reload all prompts from files (useful for updates without restart)
     */
    public void reloadPrompts() {
        log.info("🔄 Reloading all prompts...");
        promptCache.clear();
        init();
    }
}