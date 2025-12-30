package com.example.backend.analytics.service;

import com.example.backend.analytics.dto.CategoryStats;
import com.example.backend.analytics.dto.QuestionSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsFileService {

    private final ObjectMapper objectMapper;
    private static final String BASE_PATH = "storage";

    // ==================== Directory Management ====================

    public void ensureAnalyticsDirectoryExists(Long userId) {
        try {
            Path analyticsPath = getAnalyticsPath(userId);
            if (!Files.exists(analyticsPath)) {
                Files.createDirectories(analyticsPath);
                log.info("✅ Created analytics directory for user: {}", userId);
            }
        } catch (IOException e) {
            log.error("❌ Failed to create analytics directory", e);
            throw new RuntimeException("נכשל ביצירת תיקיית אנליטיקס", e);
        }
    }

    private Path getAnalyticsPath(Long userId) {
        return Paths.get(BASE_PATH, "user_" + userId, "analytics");
    }

    private Path getQuestionsFilePath(Long userId) {
        return getAnalyticsPath(userId).resolve("questions.txt");
    }

    private Path getCategoriesFilePath(Long userId) {
        return getAnalyticsPath(userId).resolve("categories.txt");
    }

    // ==================== Questions File ====================

    public void appendRawQuestions(Long userId, List<String> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        try {
            ensureAnalyticsDirectoryExists(userId);
            Path questionsFile = getQuestionsFilePath(userId);

            if (!Files.exists(questionsFile)) {
                Files.createFile(questionsFile);
            }

            for (String question : questions) {
                Files.writeString(questionsFile,
                        question + System.lineSeparator(),
                        StandardOpenOption.APPEND);
            }

            log.info("✅ Appended {} raw questions for user {}", questions.size(), userId);

        } catch (IOException e) {
            log.error("❌ Failed to append questions", e);
            throw new RuntimeException("נכשל בשמירת שאלות", e);
        }
    }

    public List<String> readRawQuestions(Long userId) {
        try {
            Path questionsFile = getQuestionsFilePath(userId);

            if (!Files.exists(questionsFile)) {
                return new ArrayList<>();
            }

            return Files.readAllLines(questionsFile);

        } catch (IOException e) {
            log.error("❌ Failed to read questions", e);
            throw new RuntimeException("נכשל בקריאת שאלות", e);
        }
    }

    public void saveProcessedQuestions(Long userId, List<QuestionSummary> summaries) {
        try {
            ensureAnalyticsDirectoryExists(userId);
            Path questionsFile = getQuestionsFilePath(userId);

            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(summaries);

            Files.writeString(questionsFile, json);

            log.info("✅ Saved {} processed questions for user {}", summaries.size(), userId);

        } catch (IOException e) {
            log.error("❌ Failed to save processed questions", e);
            throw new RuntimeException("נכשל בשמירת שאלות מעובדות", e);
        }
    }

    public List<QuestionSummary> readProcessedQuestions(Long userId) {
        try {
            Path questionsFile = getQuestionsFilePath(userId);

            if (!Files.exists(questionsFile)) {
                return new ArrayList<>();
            }

            String content = Files.readString(questionsFile);

            if (content.trim().startsWith("[")) {
                return objectMapper.readValue(content,
                        new TypeReference<List<QuestionSummary>>() {});
            } else {
                return new ArrayList<>();
            }

        } catch (IOException e) {
            log.error("❌ Failed to read processed questions", e);
            throw new RuntimeException("נכשל בקריאת שאלות מעובדות", e);
        }
    }

    public boolean areQuestionsProcessed(Long userId) {
        try {
            Path questionsFile = getQuestionsFilePath(userId);

            if (!Files.exists(questionsFile)) {
                return false;
            }

            String content = Files.readString(questionsFile);
            return content.trim().startsWith("[");

        } catch (IOException e) {
            return false;
        }
    }

    // ==================== Categories File ====================

    public void appendRawCategories(Long userId, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }

        try {
            ensureAnalyticsDirectoryExists(userId);
            Path categoriesFile = getCategoriesFilePath(userId);

            if (!Files.exists(categoriesFile)) {
                Files.createFile(categoriesFile);
            }

            for (String category : categories) {
                Files.writeString(categoriesFile,
                        category + System.lineSeparator(),
                        StandardOpenOption.APPEND);
            }

            log.info("✅ Appended {} raw categories for user {}", categories.size(), userId);

        } catch (IOException e) {
            log.error("❌ Failed to append categories", e);
            throw new RuntimeException("נכשל בשמירת קטגוריות", e);
        }
    }

    public List<String> readRawCategories(Long userId) {
        try {
            Path categoriesFile = getCategoriesFilePath(userId);

            if (!Files.exists(categoriesFile)) {
                return new ArrayList<>();
            }

            return Files.readAllLines(categoriesFile);

        } catch (IOException e) {
            log.error("❌ Failed to read categories", e);
            throw new RuntimeException("נכשל בקריאת קטגוריות", e);
        }
    }

    public void saveProcessedCategories(Long userId, List<CategoryStats> stats) {
        try {
            ensureAnalyticsDirectoryExists(userId);
            Path categoriesFile = getCategoriesFilePath(userId);

            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(stats);

            Files.writeString(categoriesFile, json);

            log.info("✅ Saved {} processed categories for user {}", stats.size(), userId);

        } catch (IOException e) {
            log.error("❌ Failed to save processed categories", e);
            throw new RuntimeException("נכשל בשמירת קטגוריות מעובדות", e);
        }
    }

    public List<CategoryStats> readProcessedCategories(Long userId) {
        try {
            Path categoriesFile = getCategoriesFilePath(userId);

            if (!Files.exists(categoriesFile)) {
                return new ArrayList<>();
            }

            String content = Files.readString(categoriesFile);

            if (content.trim().startsWith("[")) {
                return objectMapper.readValue(content,
                        new TypeReference<List<CategoryStats>>() {});
            } else {
                return new ArrayList<>();
            }

        } catch (IOException e) {
            log.error("❌ Failed to read processed categories", e);
            throw new RuntimeException("נכשל בקריאת קטגוריות מעובדות", e);
        }
    }

    public boolean areCategoriesProcessed(Long userId) {
        try {
            Path categoriesFile = getCategoriesFilePath(userId);

            if (!Files.exists(categoriesFile)) {
                return false;
            }

            String content = Files.readString(categoriesFile);
            return content.trim().startsWith("[");

        } catch (IOException e) {
            return false;
        }
    }

    // ==================== Clear Data ====================

    public void clearAllAnalytics(Long userId) {
        try {
            Path questionsFile = getQuestionsFilePath(userId);
            Path categoriesFile = getCategoriesFilePath(userId);

            if (Files.exists(questionsFile)) {
                Files.delete(questionsFile);
            }

            if (Files.exists(categoriesFile)) {
                Files.delete(categoriesFile);
            }

            log.info("✅ Cleared analytics for user {}", userId);

        } catch (IOException e) {
            log.error("❌ Failed to clear analytics", e);
            throw new RuntimeException("נכשל במחיקת אנליטיקס", e);
        }
    }
}