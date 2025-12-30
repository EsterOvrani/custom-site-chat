package com.example.backend.analytics.controller;

import com.example.backend.analytics.dto.*;
import com.example.backend.analytics.service.AnalyticsService;
import com.example.backend.collection.service.CollectionService;
import com.example.backend.user.model.User;
import com.example.backend.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final CollectionService collectionService;

    @PostMapping("/session-ended")
    public ResponseEntity<Map<String, Object>> sessionEnded(@RequestBody SessionEndedRequest request) {
        try {
            log.info("📥 Session ended request received");
            User user = collectionService.validateSecretKey(request.getSecretKey());
            analyticsService.processEndedSession(user, request.getConversationHistory());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Session processed");
            return ResponseEntity.ok(response);
        } catch (UnauthorizedException e) {
            log.error("❌ Invalid secret key");
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Invalid secret key");
            return ResponseEntity.status(401).body(response);
        } catch (Exception e) {
            log.error("❌ Failed to process session", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Internal server error");
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/questions")
    public ResponseEntity<AnalyticsResponse<List<QuestionSummary>>> getQuestions() {
        try {
            User currentUser = getCurrentUser();
            List<QuestionSummary> questions = analyticsService.getProcessedQuestions(currentUser);
            return ResponseEntity.ok(AnalyticsResponse.success(questions));
        } catch (Exception e) {
            log.error("❌ Failed to get questions", e);
            return ResponseEntity.status(500).body(AnalyticsResponse.error("שגיאה בטעינת שאלות"));
        }
    }

    @GetMapping("/questions/download")
    public ResponseEntity<Resource> downloadQuestionsExcel() {
        try {
            User currentUser = getCurrentUser();
            log.info("📥 Downloading questions Excel for user: {}", currentUser.getId());
            List<QuestionSummary> questions = analyticsService.getProcessedQuestions(currentUser);
            byte[] excelBytes = createQuestionsExcel(questions);
            ByteArrayResource resource = new ByteArrayResource(excelBytes);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=questions-report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(excelBytes.length)
                    .body(resource);
        } catch (Exception e) {
            log.error("❌ Failed to download questions Excel", e);
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/questions")
    public ResponseEntity<AnalyticsResponse<Void>> clearQuestions() {
        try {
            User currentUser = getCurrentUser();
            analyticsService.clearAllAnalytics(currentUser);
            return ResponseEntity.ok(AnalyticsResponse.success("השאלות נמחקו בהצלחה", null));
        } catch (Exception e) {
            log.error("❌ Failed to clear questions", e);
            return ResponseEntity.status(500).body(AnalyticsResponse.error("שגיאה במחיקת שאלות"));
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<AnalyticsResponse<List<CategoryStats>>> getCategories() {
        try {
            User currentUser = getCurrentUser();
            List<CategoryStats> categories = analyticsService.getProcessedCategories(currentUser);
            return ResponseEntity.ok(AnalyticsResponse.success(categories));
        } catch (Exception e) {
            log.error("❌ Failed to get categories", e);
            return ResponseEntity.status(500).body(AnalyticsResponse.error("שגיאה בטעינת קטגוריות"));
        }
    }

    @DeleteMapping("/categories")
    public ResponseEntity<AnalyticsResponse<Void>> clearCategories() {
        try {
            User currentUser = getCurrentUser();
            analyticsService.clearAllAnalytics(currentUser);
            return ResponseEntity.ok(AnalyticsResponse.success("הקטגוריות נמחקו בהצלחה", null));
        } catch (Exception e) {
            log.error("❌ Failed to clear categories", e);
            return ResponseEntity.status(500).body(AnalyticsResponse.error("שגיאה במחיקת קטגוריות"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<AnalyticsResponse<AnalyticsService.AnalyticsStats>> getStats() {
        try {
            User currentUser = getCurrentUser();
            AnalyticsService.AnalyticsStats stats = analyticsService.getStats(currentUser);
            return ResponseEntity.ok(AnalyticsResponse.success(stats));
        } catch (Exception e) {
            log.error("❌ Failed to get stats", e);
            return ResponseEntity.status(500).body(AnalyticsResponse.error("שגיאה בטעינת סטטיסטיקה"));
        }
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<AnalyticsResponse<Void>> clearAll() {
        try {
            User currentUser = getCurrentUser();
            analyticsService.clearAllAnalytics(currentUser);
            return ResponseEntity.ok(AnalyticsResponse.success("כל האנליטיקס נמחק בהצלחה", null));
        } catch (Exception e) {
            log.error("❌ Failed to clear all analytics", e);
            return ResponseEntity.status(500).body(AnalyticsResponse.error("שגיאה במחיקת אנליטיקס"));
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("משתמש לא מחובר");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new UnauthorizedException("משתמש לא תקין");
        }
        return (User) principal;
    }

    private byte[] createQuestionsExcel(List<QuestionSummary> questions) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("שאלות ללא מענה");
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Row headerRow = sheet.createRow(0);
        String[] headers = {"#", "שאלה", "כמה פעמים נשאלה", "דוגמאות לניסוחים"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        int rowNum = 1;
        for (QuestionSummary q : questions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(q.getQuestion());
            row.createCell(2).setCellValue(q.getCount());
            row.createCell(3).setCellValue(q.getExamples() != null ? String.join(", ", q.getExamples()) : "");
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }
}