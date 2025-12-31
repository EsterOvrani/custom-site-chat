package com.example.backend.analytics.service;

import com.example.backend.analytics.dto.AnalyticsStats;
import com.example.backend.analytics.dto.CategoryStats;
import com.example.backend.analytics.dto.QuestionSummary;
import com.example.backend.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Analytics Service - Business logic for analytics
 * 
 * Flow:
 * 1. Widget sends already-analyzed data (questions + topics)
 * 2. Service saves raw data to S3
 * 3. On first report request, consolidates duplicates
 * 4. Returns processed data to frontend
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AnalyticsFileService fileService;
    private final AnalyticsSummarizationService summarizationService;

    // ========================================================================
    // PROCESS ANALYTICS DATA (from Widget)
    // ========================================================================

    /**
     * Process analytics data received from widget
     * Widget already analyzed the conversation, we just save the results
     * 
     * @param user The user
     * @param unansweredQuestions List of questions (already extracted by widget)
     * @param topics List of topics/categories (already extracted by widget)
     */
    public void processAnalyticsData(
            User user,
            List<String> unansweredQuestions,
            List<String> topics) {
        
        try {
            log.info("🔵 Processing analytics for user: {}", user.getId());
            log.info("   Questions: {}", unansweredQuestions != null ? unansweredQuestions.size() : 0);
            log.info("   Topics: {}", topics != null ? topics.size() : 0);

            // Save raw questions to S3
            if (unansweredQuestions != null && !unansweredQuestions.isEmpty()) {
                fileService.appendRawQuestions(user.getId(), unansweredQuestions);
                log.info("✅ Saved {} raw questions", unansweredQuestions.size());
            }

            // Save raw categories to S3
            if (topics != null && !topics.isEmpty()) {
                fileService.appendRawCategories(user.getId(), topics);
                log.info("✅ Saved {} raw categories", topics.size());
            }

            log.info("✅ Analytics processed successfully");

        } catch (Exception e) {
            log.error("❌ Failed to process analytics", e);
            throw new RuntimeException("נכשל בעיבוד אנליטיקס", e);
        }
    }

    // ========================================================================
    // GET PROCESSED QUESTIONS
    // ========================================================================

    /**
     * Get processed questions (consolidated with counts)
     * First request triggers summarization via OpenAI
     * Subsequent requests return cached JSON
     */
    public List<QuestionSummary> getProcessedQuestions(Long userId) {
        try {
            log.info("📊 Getting processed questions for user: {}", userId);

            // Check if already processed
            if (fileService.areQuestionsProcessed(userId)) {
                log.info("✅ Questions already processed, reading from S3");
                return fileService.readProcessedQuestions(userId);
            }

            // Read raw questions
            List<String> rawQuestions = fileService.readRawQuestions(userId);
            
            if (rawQuestions.isEmpty()) {
                log.info("📭 No questions found");
                return List.of();
            }

            log.info("🔄 Processing {} raw questions...", rawQuestions.size());

            // Summarize with OpenAI
            List<QuestionSummary> summaries = summarizationService.summarizeQuestions(rawQuestions);

            // Save processed results
            fileService.saveProcessedQuestions(userId, summaries);

            log.info("✅ Processed {} questions → {} summaries", rawQuestions.size(), summaries.size());
            return summaries;

        } catch (Exception e) {
            log.error("❌ Failed to get processed questions", e);
            throw new RuntimeException("נכשל בקריאת שאלות", e);
        }
    }

    // ========================================================================
    // GET PROCESSED CATEGORIES
    // ========================================================================

    /**
     * Get processed categories (consolidated with stats)
     * First request triggers summarization via OpenAI
     * Subsequent requests return cached JSON
     */
    public List<CategoryStats> getProcessedCategories(Long userId) {
        try {
            log.info("📊 Getting processed categories for user: {}", userId);

            // Check if already processed
            if (fileService.areCategoriesProcessed(userId)) {
                log.info("✅ Categories already processed, reading from S3");
                return fileService.readProcessedCategories(userId);
            }

            // Read raw categories
            List<String> rawCategories = fileService.readRawCategories(userId);
            
            if (rawCategories.isEmpty()) {
                log.info("📭 No categories found");
                return List.of();
            }

            log.info("🔄 Processing {} raw categories...", rawCategories.size());

            // Summarize with OpenAI
            List<CategoryStats> stats = summarizationService.summarizeCategories(rawCategories);

            // Save processed results
            fileService.saveProcessedCategories(userId, stats);

            log.info("✅ Processed {} categories → {} stats", rawCategories.size(), stats.size());
            return stats;

        } catch (Exception e) {
            log.error("❌ Failed to get processed categories", e);
            throw new RuntimeException("נכשל בקריאת קטגוריות", e);
        }
    }

    // ========================================================================
    // CLEAR DATA
    // ========================================================================

    /**
     * Clear questions data
     */
    public void clearQuestions(Long userId) {
        try {
            log.info("🗑️ Clearing questions for user: {}", userId);
            
            // Just delete the file from S3
            fileService.clearQuestions(userId);
            
            log.info("✅ Questions cleared successfully");

        } catch (Exception e) {
            log.error("❌ Failed to clear questions", e);
            throw new RuntimeException("נכשל במחיקת שאלות", e);
        }
    }

    /**
     * Clear categories data
     */
    public void clearCategories(Long userId) {
        try {
            log.info("🗑️ Clearing categories for user: {}", userId);
            
            // Just delete the file from S3
            fileService.clearCategories(userId);
            
            log.info("✅ Categories cleared successfully");

        } catch (Exception e) {
            log.error("❌ Failed to clear categories", e);
            throw new RuntimeException("נכשל במחיקת קטגוריות", e);
        }
    }

    /**
     * Clear all analytics data
     */
    public void clearAllAnalytics(Long userId) {
        try {
            log.info("🗑️ Clearing all analytics for user: {}", userId);
            
            fileService.clearAllAnalytics(userId);
            
            log.info("✅ All analytics cleared successfully");

        } catch (Exception e) {
            log.error("❌ Failed to clear all analytics", e);
            throw new RuntimeException("נכשל במחיקת אנליטיקס", e);
        }
    }

    // ========================================================================
    // GET STATISTICS
    // ========================================================================

    /**
     * Get summary statistics
     * Note: No longer tracking "total sessions" - only question counts
     */
    public AnalyticsStats getStats(Long userId) {
        try {
            log.info("📊 Getting stats for user: {}", userId);

            AnalyticsStats stats = new AnalyticsStats();

            // Count raw questions
            List<String> rawQuestions = fileService.readRawQuestions(userId);
            stats.setTotalQuestions(rawQuestions.size());

            // Count unique questions (from processed data if available)
            if (fileService.areQuestionsProcessed(userId)) {
                List<QuestionSummary> processed = fileService.readProcessedQuestions(userId);
                stats.setUniqueQuestions(processed.size());
                stats.setQuestionsProcessed(true);
            } else {
                stats.setUniqueQuestions(0);
                stats.setQuestionsProcessed(false);
            }

            // Count categories
            List<String> rawCategories = fileService.readRawCategories(userId);
            stats.setTotalCategories(rawCategories.size());

            if (fileService.areCategoriesProcessed(userId)) {
                List<CategoryStats> processed = fileService.readProcessedCategories(userId);
                stats.setUniqueCategories(processed.size());
                stats.setCategoriesProcessed(true);
            } else {
                stats.setUniqueCategories(0);
                stats.setCategoriesProcessed(false);
            }

            log.info("✅ Stats: {} questions, {} categories", 
                stats.getTotalQuestions(), stats.getTotalCategories());

            return stats;

        } catch (Exception e) {
            log.error("❌ Failed to get stats", e);
            throw new RuntimeException("נכשל בקריאת סטטיסטיקות", e);
        }
    }
}