package com.example.backend.analytics.service;

import com.example.backend.analytics.dto.CategoryStats;
import com.example.backend.analytics.dto.QuestionSummary;
import com.example.backend.common.infrastructure.storage.S3Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Analytics File Service - Manages analytics data in S3
 * 
 * File structure in S3:
 * user/{id}/analytics/questions.txt   (raw or processed JSON)
 * user/{id}/analytics/categories.txt  (raw or processed JSON)
 * 
 * Flow:
 * 1. Raw data is appended as text (one per line)
 * 2. On first report request, OpenAI consolidates → saves as JSON
 * 3. File detection: starts with "[" = processed JSON
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsFileService {

    private final ObjectMapper objectMapper;
    private final S3Service s3Service;
    
    // ==================== S3 Key Generation ====================
    
    private String getAnalyticsPrefix(Long userId) {
        return "users/" + userId + "/analytics/";
    }
    
    private String getQuestionsKey(Long userId) {
        return getAnalyticsPrefix(userId) + "questions.txt";
    }
    
    private String getCategoriesKey(Long userId) {
        return getAnalyticsPrefix(userId) + "categories.txt";
    }
    
    // ==================== Questions - RAW ====================
    
    public void appendRawQuestions(Long userId, List<String> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        
        try {
            String key = getQuestionsKey(userId);
            
            // 1. Read existing content (if exists)
            String existingContent = "";
            if (s3Service.fileExists(key)) {
                try (InputStream is = s3Service.downloadFile(key)) {
                    existingContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } else {
                log.info("Creating new questions file for user {}", userId);
            }
            
            // 2. Append new lines
            StringBuilder newContent = new StringBuilder(existingContent);
            for (String question : questions) {
                newContent.append(question).append("\n");
            }
            
            // 3. Save back to S3
            byte[] bytes = newContent.toString().getBytes(StandardCharsets.UTF_8);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                s3Service.uploadFile(bais, key, "text/plain; charset=utf-8", bytes.length);
            }
            
            log.info("✅ Appended {} raw questions for user {} to S3", questions.size(), userId);
            
        } catch (Exception e) {
            log.error("❌ Failed to append questions to S3", e);
            throw new RuntimeException("נכשל בשמירת שאלות ל-S3", e);
        }
    }
    
    public List<String> readRawQuestions(Long userId) {
        try {
            String key = getQuestionsKey(userId);
            
            if (!s3Service.fileExists(key)) {
                log.info("No questions file found for user {}", userId);
                return new ArrayList<>();
            }
            
            String content;
            try (InputStream is = s3Service.downloadFile(key)) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            
            // Split to lines
            return content.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("❌ Failed to read questions from S3", e);
            throw new RuntimeException("נכשל בקריאת שאלות מ-S3", e);
        }
    }
    
    // ==================== Questions - PROCESSED ====================
    
    public void saveProcessedQuestions(Long userId, List<QuestionSummary> summaries) {
        try {
            String key = getQuestionsKey(userId);
            
            // Convert to JSON
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(summaries);
            
            // Save to S3
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                s3Service.uploadFile(bais, key, "application/json; charset=utf-8", bytes.length);
            }
            
            log.info("✅ Saved {} processed questions for user {} to S3", summaries.size(), userId);
            
        } catch (Exception e) {
            log.error("❌ Failed to save processed questions to S3", e);
            throw new RuntimeException("נכשל בשמירת שאלות מעובדות ל-S3", e);
        }
    }
    
    public List<QuestionSummary> readProcessedQuestions(Long userId) {
        try {
            String key = getQuestionsKey(userId);
            
            if (!s3Service.fileExists(key)) {
                log.info("No processed questions found for user {}", userId);
                return new ArrayList<>();
            }
            
            String content;
            try (InputStream is = s3Service.downloadFile(key)) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            
            // Check if JSON
            if (content.trim().startsWith("[")) {
                return objectMapper.readValue(content, 
                    new TypeReference<List<QuestionSummary>>() {});
            } else {
                // Still raw text file
                return new ArrayList<>();
            }
            
        } catch (IOException e) {
            log.error("❌ Failed to read processed questions from S3", e);
            throw new RuntimeException("נכשל בקריאת שאלות מעובדות מ-S3", e);
        }
    }
    
    public boolean areQuestionsProcessed(Long userId) {
        try {
            String key = getQuestionsKey(userId);
            
            if (!s3Service.fileExists(key)) {
                return false;
            }
            
            String content;
            try (InputStream is = s3Service.downloadFile(key)) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            
            return content.trim().startsWith("[");
            
        } catch (Exception e) {
            log.error("❌ Failed to check questions status", e);
            return false;
        }
    }
    
    // ==================== Categories - RAW ====================
    
    public void appendRawCategories(Long userId, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }
        
        try {
            String key = getCategoriesKey(userId);
            
            // 1. Read existing content
            String existingContent = "";
            if (s3Service.fileExists(key)) {
                try (InputStream is = s3Service.downloadFile(key)) {
                    existingContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } else {
                log.info("Creating new categories file for user {}", userId);
            }
            
            // 2. Append new lines
            StringBuilder newContent = new StringBuilder(existingContent);
            for (String category : categories) {
                newContent.append(category).append("\n");
            }
            
            // 3. Save back
            byte[] bytes = newContent.toString().getBytes(StandardCharsets.UTF_8);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                s3Service.uploadFile(bais, key, "text/plain; charset=utf-8", bytes.length);
            }
            
            log.info("✅ Appended {} raw categories for user {} to S3", categories.size(), userId);
            
        } catch (Exception e) {
            log.error("❌ Failed to append categories to S3", e);
            throw new RuntimeException("נכשל בשמירת קטגוריות ל-S3", e);
        }
    }
    
    public List<String> readRawCategories(Long userId) {
        try {
            String key = getCategoriesKey(userId);
            
            if (!s3Service.fileExists(key)) {
                log.info("No categories file found for user {}", userId);
                return new ArrayList<>();
            }
            
            String content;
            try (InputStream is = s3Service.downloadFile(key)) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            
            return content.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("❌ Failed to read categories from S3", e);
            throw new RuntimeException("נכשל בקריאת קטגוריות מ-S3", e);
        }
    }
    
    // ==================== Categories - PROCESSED ====================
    
    public void saveProcessedCategories(Long userId, List<CategoryStats> stats) {
        try {
            String key = getCategoriesKey(userId);
            
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(stats);
            
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                s3Service.uploadFile(bais, key, "application/json; charset=utf-8", bytes.length);
            }
            
            log.info("✅ Saved {} processed categories for user {} to S3", stats.size(), userId);
            
        } catch (Exception e) {
            log.error("❌ Failed to save processed categories to S3", e);
            throw new RuntimeException("נכשל בשמירת קטגוריות מעובדות ל-S3", e);
        }
    }
    
    public List<CategoryStats> readProcessedCategories(Long userId) {
        try {
            String key = getCategoriesKey(userId);
            
            if (!s3Service.fileExists(key)) {
                log.info("No processed categories found for user {}", userId);
                return new ArrayList<>();
            }
            
            String content;
            try (InputStream is = s3Service.downloadFile(key)) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            
            if (content.trim().startsWith("[")) {
                return objectMapper.readValue(content, 
                    new TypeReference<List<CategoryStats>>() {});
            } else {
                return new ArrayList<>();
            }
            
        } catch (IOException e) {
            log.error("❌ Failed to read processed categories from S3", e);
            throw new RuntimeException("נכשל בקריאת קטגוריות מעובדות מ-S3", e);
        }
    }
    
    public boolean areCategoriesProcessed(Long userId) {
        try {
            String key = getCategoriesKey(userId);
            
            if (!s3Service.fileExists(key)) {
                return false;
            }
            
            String content;
            try (InputStream is = s3Service.downloadFile(key)) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            
            return content.trim().startsWith("[");
            
        } catch (Exception e) {
            log.error("❌ Failed to check categories status", e);
            return false;
        }
    }
    
    // ==================== Clear Data ====================
    
    public void clearQuestions(Long userId) {
        try {
            String questionsKey = getQuestionsKey(userId);
            
            if (s3Service.fileExists(questionsKey)) {
                s3Service.deleteFile(questionsKey);
                log.info("✅ Cleared questions for user {} from S3", userId);
            } else {
                log.info("📭 No questions file to clear for user {}", userId);
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to clear questions from S3", e);
            throw new RuntimeException("נכשל במחיקת שאלות מ-S3", e);
        }
    }
    
    public void clearCategories(Long userId) {
        try {
            String categoriesKey = getCategoriesKey(userId);
            
            if (s3Service.fileExists(categoriesKey)) {
                s3Service.deleteFile(categoriesKey);
                log.info("✅ Cleared categories for user {} from S3", userId);
            } else {
                log.info("📭 No categories file to clear for user {}", userId);
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to clear categories from S3", e);
            throw new RuntimeException("נכשל במחיקת קטגוריות מ-S3", e);
        }
    }
    
    public void clearAllAnalytics(Long userId) {
        try {
            clearQuestions(userId);
            clearCategories(userId);
            log.info("✅ Cleared all analytics for user {} from S3", userId);
            
        } catch (Exception e) {
            log.error("❌ Failed to clear all analytics from S3", e);
            throw new RuntimeException("נכשל במחיקת כל האנליטיקס מ-S3", e);
        }
    }
}