package com.example.backend.analytics.service;

import com.example.backend.analytics.dto.AnalyticsStats;
import com.example.backend.analytics.dto.CategoryStats;
import com.example.backend.analytics.dto.QuestionSummary;
import com.example.backend.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analytics Service with Smart Caching
 * 
 * Flow:
 * 1. Widget sends data → save as raw text → cache invalidated
 * 2. User opens dashboard → check if file changed → use cache or re-process
 * 3. Cache is user-specific and invalidates when S3 file changes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AnalyticsFileService fileService;
    private final AnalyticsSummarizationService summarizationService;

    // ========================================================================
    // CACHE STORAGE (In-Memory)
    // ========================================================================

    // Cache for processed questions
    private final Map<Long, CachedQuestions> questionsCache = new ConcurrentHashMap<>();
    
    // Cache for processed categories
    private final Map<Long, CachedCategories> categoriesCache = new ConcurrentHashMap<>();

    // Helper classes for cache entries
    private static class CachedQuestions {
        List<QuestionSummary> data;
        Instant lastModified;
        int rawCount; // Number of raw questions when cached
        
        CachedQuestions(List<QuestionSummary> data, int rawCount) {
            this.data = data;
            this.rawCount = rawCount;
            this.lastModified = Instant.now();
        }
    }
    
    private static class CachedCategories {
        List<CategoryStats> data;
        Instant lastModified;
        int rawCount; // Number of raw categories when cached
        
        CachedCategories(List<CategoryStats> data, int rawCount) {
            this.data = data;
            this.rawCount = rawCount;
            this.lastModified = Instant.now();
        }
    }

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
            log.info("🔵 Saving raw analytics for user: {}", user.getId());
            log.info("   Questions: {}", unansweredQuestions != null ? unansweredQuestions.size() : 0);
            log.info("   Topics: {}", topics != null ? topics.size() : 0);

            Long userId = user.getId();

            // Save raw questions to S3
            if (unansweredQuestions != null && !unansweredQuestions.isEmpty()) {
                fileService.appendRawQuestions(userId, unansweredQuestions);
                log.info("✅ Saved {} raw questions", unansweredQuestions.size());
                
                // Invalidate cache since new data arrived
                questionsCache.remove(userId);
                log.info("🔄 Questions cache invalidated for user {}", userId);
            }

            // Save raw categories to S3
            if (topics != null && !topics.isEmpty()) {
                fileService.appendRawCategories(userId, topics);
                log.info("✅ Saved {} raw categories", topics.size());
                
                // Invalidate cache since new data arrived
                categoriesCache.remove(userId);
                log.info("🔄 Categories cache invalidated for user {}", userId);
            }

            log.info("✅ Raw analytics saved successfully");

        } catch (Exception e) {
            log.error("❌ Failed to save analytics", e);
            throw new RuntimeException("נכשל בשמירת אנליטיקס", e);
        }
    }

    // ========================================================================
    // GET PROCESSED QUESTIONS (Dashboard Request with Smart Cache)
    // ========================================================================

    /**
     * Get processed questions with smart caching
     * Returns cached result if data hasn't changed
     * Re-processes with OpenAI if new data arrived
     */
    public List<QuestionSummary> getProcessedQuestions(Long userId) {
        try {
            log.info("📊 Getting processed questions for user: {}", userId);

            // Read raw questions from S3
            List<String> rawQuestions = fileService.readRawQuestions(userId);
            
            if (rawQuestions.isEmpty()) {
                log.info("📭 No questions found");
                return List.of();
            }

            int currentRawCount = rawQuestions.size();

            // Check cache
            CachedQuestions cached = questionsCache.get(userId);
            
            if (cached != null && cached.rawCount == currentRawCount) {
                // Cache is valid - same number of questions
                long cacheAge = Instant.now().getEpochSecond() - cached.lastModified.getEpochSecond();
                log.info("✅ Using cached questions (age: {}s, count: {})", cacheAge, currentRawCount);
                return cached.data;
            }

            // Cache miss or invalidated - process with OpenAI
            log.info("🔄 Processing {} raw questions with OpenAI (cache miss)...", currentRawCount);
            List<QuestionSummary> summaries = summarizationService.summarizeQuestions(rawQuestions);

            // Update cache
            questionsCache.put(userId, new CachedQuestions(summaries, currentRawCount));
            log.info("✅ Processed and cached {} questions → {} summaries", currentRawCount, summaries.size());
            
            return summaries;

        } catch (Exception e) {
            log.error("❌ Failed to process questions", e);
            throw new RuntimeException("נכשל בעיבוד שאלות", e);
        }
    }

    // ========================================================================
    // GET PROCESSED CATEGORIES (Dashboard Request with Smart Cache)
    // ========================================================================

    /**
     * Get processed categories with smart caching
     * Returns cached result if data hasn't changed
     * Re-processes with OpenAI if new data arrived
     */
    public List<CategoryStats> getProcessedCategories(Long userId) {
        try {
            log.info("📊 Getting processed categories for user: {}", userId);

            // Read raw categories from S3
            List<String> rawCategories = fileService.readRawCategories(userId);
            
            if (rawCategories.isEmpty()) {
                log.info("📭 No categories found");
                return List.of();
            }

            int currentRawCount = rawCategories.size();

            // Check cache
            CachedCategories cached = categoriesCache.get(userId);
            
            if (cached != null && cached.rawCount == currentRawCount) {
                // Cache is valid - same number of categories
                long cacheAge = Instant.now().getEpochSecond() - cached.lastModified.getEpochSecond();
                log.info("✅ Using cached categories (age: {}s, count: {})", cacheAge, currentRawCount);
                return cached.data;
            }

            // Cache miss or invalidated - process with OpenAI
            log.info("🔄 Processing {} raw categories with OpenAI (cache miss)...", currentRawCount);
            List<CategoryStats> stats = summarizationService.summarizeCategories(rawCategories);

            // Update cache
            categoriesCache.put(userId, new CachedCategories(stats, currentRawCount));
            log.info("✅ Processed and cached {} categories → {} stats", currentRawCount, stats.size());
            
            return stats;

        } catch (Exception e) {
            log.error("❌ Failed to process categories", e);
            throw new RuntimeException("נכשל בעיבוד קטגוריות", e);
        }
    }

    // ========================================================================
    // CLEAR DATA (Also clears cache)
    // ========================================================================

    public void clearQuestions(Long userId) {
        try {
            log.info("🗑️ Clearing questions for user: {}", userId);
            fileService.clearQuestions(userId);
            questionsCache.remove(userId); // Clear cache
            log.info("✅ Questions and cache cleared successfully");
        } catch (Exception e) {
            log.error("❌ Failed to clear questions", e);
            throw new RuntimeException("נכשל במחיקת שאלות", e);
        }
    }

    public void clearCategories(Long userId) {
        try {
            log.info("🗑️ Clearing categories for user: {}", userId);
            fileService.clearCategories(userId);
            categoriesCache.remove(userId); // Clear cache
            log.info("✅ Categories and cache cleared successfully");
        } catch (Exception e) {
            log.error("❌ Failed to clear categories", e);
            throw new RuntimeException("נכשל במחיקת קטגוריות", e);
        }
    }

    public void clearAllAnalytics(Long userId) {
        try {
            log.info("🗑️ Clearing all analytics for user: {}", userId);
            fileService.clearAllAnalytics(userId);
            questionsCache.remove(userId); // Clear cache
            categoriesCache.remove(userId); // Clear cache
            log.info("✅ All analytics and cache cleared successfully");
        } catch (Exception e) {
            log.error("❌ Failed to clear all analytics", e);
            throw new RuntimeException("נכשל במחיקת אנליטיקס", e);
        }
    }

    // ========================================================================
    // GET STATISTICS
    // ========================================================================

    public AnalyticsStats getStats(Long userId) {
        try {
            log.info("📊 Getting stats for user: {}", userId);

            AnalyticsStats stats = new AnalyticsStats();

            // Count raw questions
            List<String> rawQuestions = fileService.readRawQuestions(userId);
            stats.setTotalQuestions(rawQuestions.size());
            
            // Check if cached
            CachedQuestions cachedQ = questionsCache.get(userId);
            if (cachedQ != null) {
                stats.setUniqueQuestions(cachedQ.data.size());
                stats.setQuestionsProcessed(true);
            } else {
                stats.setUniqueQuestions(0);
                stats.setQuestionsProcessed(false);
            }

            // Count raw categories
            List<String> rawCategories = fileService.readRawCategories(userId);
            stats.setTotalCategories(rawCategories.size());
            
            // Check if cached
            CachedCategories cachedC = categoriesCache.get(userId);
            if (cachedC != null) {
                stats.setUniqueCategories(cachedC.data.size());
                stats.setCategoriesProcessed(true);
            } else {
                stats.setUniqueCategories(0);
                stats.setCategoriesProcessed(false);
            }

            log.info("✅ Stats: {} raw questions ({} cached), {} raw categories ({} cached)", 
                stats.getTotalQuestions(), 
                cachedQ != null ? "yes" : "no",
                stats.getTotalCategories(),
                cachedC != null ? "yes" : "no");

            return stats;

        } catch (Exception e) {
            log.error("❌ Failed to get stats", e);
            throw new RuntimeException("נכשל בקריאת סטטיסטיקות", e);
        }
    }
}