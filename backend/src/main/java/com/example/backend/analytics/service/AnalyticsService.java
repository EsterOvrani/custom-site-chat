package com.example.backend.analytics.service;

import com.example.backend.analytics.dto.CategoryStats;
import com.example.backend.analytics.dto.QuestionSummary;
import com.example.backend.analytics.dto.SessionEndedRequest;
import com.example.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {
    private final OpenAIAnalysisService openAIService;
    private final AnalyticsFileService fileService;

    @Async("documentProcessingExecutor")
    public void processEndedSession(User user, List<SessionEndedRequest.ConversationMessage> conversation) {
        try {
            log.info("🔵 Processing ended session for user: {}", user.getId());
            String businessType = user.getBusinessType() != null ? user.getBusinessType() : "אתר כללי";

            List<String> unansweredQuestions = openAIService.extractUnansweredQuestions(conversation, businessType);
            List<String> topics = openAIService.extractTopics(conversation, businessType);

            if (!unansweredQuestions.isEmpty()) {
                fileService.appendRawQuestions(user.getId(), unansweredQuestions);
            }
            if (!topics.isEmpty()) {
                fileService.appendRawCategories(user.getId(), topics);
            }

            log.info("✅ Session processed successfully - {} questions, {} topics", unansweredQuestions.size(), topics.size());
        } catch (Exception e) {
            log.error("❌ Failed to process ended session", e);
        }
    }

    public List<QuestionSummary> getProcessedQuestions(User user) {
        log.info("📊 Getting processed questions for user: {}", user.getId());
        if (fileService.areQuestionsProcessed(user.getId())) {
            log.info("✅ Questions already processed, reading from file");
            return fileService.readProcessedQuestions(user.getId());
        }
        log.info("🔄 Questions not processed yet, processing now...");
        List<String> rawQuestions = fileService.readRawQuestions(user.getId());
        if (rawQuestions.isEmpty()) {
            log.info("📭 No raw questions found");
            return List.of();
        }
        String businessType = user.getBusinessType() != null ? user.getBusinessType() : "אתר כללי";
        List<QuestionSummary> summaries = openAIService.summarizeQuestions(rawQuestions, businessType);
        fileService.saveProcessedQuestions(user.getId(), summaries);
        log.info("✅ Questions processed and saved");
        return summaries;
    }

    public List<CategoryStats> getProcessedCategories(User user) {
        log.info("📊 Getting processed categories for user: {}", user.getId());
        if (fileService.areCategoriesProcessed(user.getId())) {
            log.info("✅ Categories already processed, reading from file");
            return fileService.readProcessedCategories(user.getId());
        }
        log.info("🔄 Categories not processed yet, processing now...");
        List<String> rawCategories = fileService.readRawCategories(user.getId());
        if (rawCategories.isEmpty()) {
            log.info("📭 No raw categories found");
            return List.of();
        }
        String businessType = user.getBusinessType() != null ? user.getBusinessType() : "אתר כללי";
        List<CategoryStats> stats = openAIService.summarizeCategories(rawCategories, businessType);
        fileService.saveProcessedCategories(user.getId(), stats);
        log.info("✅ Categories processed and saved");
        return stats;
    }

    public void clearAllAnalytics(User user) {
        log.info("🗑️ Clearing all analytics for user: {}", user.getId());
        fileService.clearAllAnalytics(user.getId());
        log.info("✅ All analytics cleared");
    }

    public AnalyticsStats getStats(User user) {
        List<String> rawQuestions = fileService.readRawQuestions(user.getId());
        List<String> rawCategories = fileService.readRawCategories(user.getId());
        boolean questionsProcessed = fileService.areQuestionsProcessed(user.getId());
        boolean categoriesProcessed = fileService.areCategoriesProcessed(user.getId());

        return AnalyticsStats.builder()
                .totalSessions(rawCategories.size())
                .totalQuestions(rawQuestions.size())
                .uniqueQuestions(questionsProcessed ? fileService.readProcessedQuestions(user.getId()).size() : 0)
                .totalCategories(rawCategories.size())
                .uniqueCategories(categoriesProcessed ? fileService.readProcessedCategories(user.getId()).size() : 0)
                .questionsProcessed(questionsProcessed)
                .categoriesProcessed(categoriesProcessed)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class AnalyticsStats {
        private Integer totalSessions;
        private Integer totalQuestions;
        private Integer uniqueQuestions;
        private Integer totalCategories;
        private Integer uniqueCategories;
        private Boolean questionsProcessed;
        private Boolean categoriesProcessed;
    }
}