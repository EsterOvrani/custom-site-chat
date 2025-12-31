package com.example.backend.analytics.controller;

import com.example.backend.analytics.dto.AnalyticsResponse;
import com.example.backend.analytics.dto.AnalyticsStats;
import com.example.backend.analytics.dto.CategoryStats;
import com.example.backend.analytics.dto.QuestionSummary;
import com.example.backend.analytics.service.AnalyticsService;
import com.example.backend.collection.service.CollectionService;
import com.example.backend.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analytics Controller - Handles analytics endpoints
 * 
 * Public Endpoints (no JWT required):
 * - POST /api/analytics/session-ended - Receive analytics data from widget
 * 
 * Authenticated Endpoints (JWT required):
 * - GET /api/analytics/questions - Get questions list (with smart caching)
 * - GET /api/analytics/questions/download - Download questions as Excel
 * - DELETE /api/analytics/questions - Clear questions
 * - GET /api/analytics/categories - Get categories with stats (with smart caching)
 * - DELETE /api/analytics/categories - Clear categories
 * - GET /api/analytics/stats - Get summary statistics
 * - DELETE /api/analytics/clear-all - Clear all analytics data
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CollectionService collectionService;

    // ========================================================================
    // PUBLIC ENDPOINTS (No JWT required - validated by secretKey)
    // ========================================================================

    /**
     * Receive analytics data from widget
     * Called when user closes chat or starts new conversation
     * 
     * Request Body:
     * {
     *   "secretKey": "user-secret-key",
     *   "unansweredQuestions": ["שאלה 1", "שאלה 2"],
     *   "topics": ["נושא 1", "נושא 2"]
     * }
     */
    @PostMapping("/session-ended")
    public ResponseEntity<Map<String, Object>> sessionEnded(@RequestBody SessionEndedRequest request) {
        try {
            log.info("📥 Analytics data received from widget");

            // Validate secret key and get user
            User user = collectionService.validateSecretKey(request.getSecretKey());

            // Process analytics data (saves raw + invalidates cache)
            analyticsService.processAnalyticsData(
                user,
                request.getUnansweredQuestions(),
                request.getTopics()
            );

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Analytics data received");

            log.info("✅ Analytics processed successfully for user: {}", user.getId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid secret key: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Invalid secret key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (Exception e) {
            log.error("❌ Failed to process analytics", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ========================================================================
    // AUTHENTICATED ENDPOINTS (JWT required)
    // ========================================================================

    /**
     * Get questions list (processed with smart caching)
     * Returns consolidated questions with counts and examples
     * Uses cache if available, processes with OpenAI if cache invalid
     */
    @GetMapping("/questions")
    public ResponseEntity<AnalyticsResponse<List<QuestionSummary>>> getQuestions(
            @AuthenticationPrincipal User user) {
        
        try {
            log.info("📊 Fetching questions for user: {}", user.getId());
            
            List<QuestionSummary> questions = analyticsService.getProcessedQuestions(user.getId());
            
            return ResponseEntity.ok(
                AnalyticsResponse.success("Questions retrieved successfully", questions)
            );

        } catch (Exception e) {
            log.error("❌ Failed to fetch questions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AnalyticsResponse.error("Failed to retrieve questions")
            );
        }
    }

    /**
     * Download questions as Excel file
     * Returns .xlsx file with questions data
     */
    @GetMapping("/questions/download")
    public ResponseEntity<byte[]> downloadQuestionsExcel(@AuthenticationPrincipal User user) {
        try {
            log.info("📥 Generating Excel for user: {}", user.getId());

            List<QuestionSummary> questions = analyticsService.getProcessedQuestions(user.getId());

            // Create Excel file
            byte[] excelBytes = createQuestionsExcel(questions);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "analytics-questions.xlsx");

            log.info("✅ Excel generated successfully ({} bytes)", excelBytes.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (Exception e) {
            log.error("❌ Failed to generate Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Clear questions data (clears both S3 and cache)
     */
    @DeleteMapping("/questions")
    public ResponseEntity<AnalyticsResponse<Void>> clearQuestions(@AuthenticationPrincipal User user) {
        try {
            log.info("🗑️ Clearing questions for user: {}", user.getId());
            
            analyticsService.clearQuestions(user.getId());
            
            return ResponseEntity.ok(
                AnalyticsResponse.success("Questions cleared successfully", null)
            );

        } catch (Exception e) {
            log.error("❌ Failed to clear questions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AnalyticsResponse.error("Failed to clear questions")
            );
        }
    }

    /**
     * Get categories with statistics (processed with smart caching)
     * Returns categories with counts and percentages
     * Uses cache if available, processes with OpenAI if cache invalid
     */
    @GetMapping("/categories")
    public ResponseEntity<AnalyticsResponse<List<CategoryStats>>> getCategories(
            @AuthenticationPrincipal User user) {
        
        try {
            log.info("📊 Fetching categories for user: {}", user.getId());
            
            List<CategoryStats> categories = analyticsService.getProcessedCategories(user.getId());
            
            return ResponseEntity.ok(
                AnalyticsResponse.success("Categories retrieved successfully", categories)
            );

        } catch (Exception e) {
            log.error("❌ Failed to fetch categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AnalyticsResponse.error("Failed to retrieve categories")
            );
        }
    }

    /**
     * Clear categories data (clears both S3 and cache)
     */
    @DeleteMapping("/categories")
    public ResponseEntity<AnalyticsResponse<Void>> clearCategories(@AuthenticationPrincipal User user) {
        try {
            log.info("🗑️ Clearing categories for user: {}", user.getId());
            
            analyticsService.clearCategories(user.getId());
            
            return ResponseEntity.ok(
                AnalyticsResponse.success("Categories cleared successfully", null)
            );

        } catch (Exception e) {
            log.error("❌ Failed to clear categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AnalyticsResponse.error("Failed to clear categories")
            );
        }
    }

    /**
     * Get summary statistics
     * Returns counts and cache status
     */
    @GetMapping("/stats")
    public ResponseEntity<AnalyticsResponse<AnalyticsStats>> getStats(
            @AuthenticationPrincipal User user) {
        
        try {
            log.info("📊 Fetching stats for user: {}", user.getId());
            
            AnalyticsStats stats = analyticsService.getStats(user.getId());
            
            return ResponseEntity.ok(
                AnalyticsResponse.success("Stats retrieved successfully", stats)
            );

        } catch (Exception e) {
            log.error("❌ Failed to fetch stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AnalyticsResponse.error("Failed to retrieve stats")
            );
        }
    }

    /**
     * Clear all analytics data (questions + categories + cache)
     */
    @DeleteMapping("/clear-all")
    public ResponseEntity<AnalyticsResponse<Void>> clearAll(@AuthenticationPrincipal User user) {
        try {
            log.info("🗑️ Clearing all analytics for user: {}", user.getId());
            
            analyticsService.clearAllAnalytics(user.getId());
            
            return ResponseEntity.ok(
                AnalyticsResponse.success("All analytics cleared successfully", null)
            );

        } catch (Exception e) {
            log.error("❌ Failed to clear all analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AnalyticsResponse.error("Failed to clear analytics")
            );
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Create Excel file from questions data
     */
    private byte[] createQuestionsExcel(List<QuestionSummary> questions) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("שאלות ללא מענה");

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {"#", "שאלה", "כמה פעמים נשאלה", "דוגמאות לניסוחים"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add data rows
            int rowNum = 1;
            for (QuestionSummary question : questions) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(question.getQuestion());
                row.createCell(2).setCellValue(question.getCount());
                
                // Join examples with comma
                String examples = question.getExamples() != null 
                    ? String.join(", ", question.getExamples())
                    : "";
                row.createCell(3).setCellValue(examples);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // ========================================================================
    // DTOs (Inner Classes)
    // ========================================================================

    /**
     * Request DTO for session-ended endpoint
     */
    public static class SessionEndedRequest {
        private String secretKey;
        private List<String> unansweredQuestions;
        private List<String> topics;

        // Getters and Setters
        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public List<String> getUnansweredQuestions() {
            return unansweredQuestions;
        }

        public void setUnansweredQuestions(List<String> unansweredQuestions) {
            this.unansweredQuestions = unansweredQuestions;
        }

        public List<String> getTopics() {
            return topics;
        }

        public void setTopics(List<String> topics) {
            this.topics = topics;
        }
    }
}