package com.example.backend.analytics.controller;

import com.example.backend.analytics.dto.AnalysisResponse;
import com.example.backend.analytics.service.AnalyticsService;
import com.example.backend.analytics.dto.SaveQuestionsRequest;
import com.example.backend.user.model.User;
import com.example.backend.common.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;



@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Receiving questions from the widget
     * This is the endpoint the widget sends the questions to
     */
    @PostMapping("/save-questions")
    public ResponseEntity<Map<String, Object>> saveQuestions(
            @RequestBody SaveQuestionsRequest request) {

        log.info("📥 Received {} questions for category: {}",
                request.getQuestions().size(),
                request.getSiteCategory());

        // find the user by secretKey
        User user = analyticsService.getUserBySecretKey(request.getSecretKey());

        // save the question with automatic filter
        analyticsService.appendQuestionsToFile(
                user,
                request.getQuestions(),
                request.getSiteCategory()
        );

        // return the respone
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    /**
     * Download questions file
     * It's happend when the website owner press on "הורד קובץ" button un the deashbard page
     */
    @GetMapping("/download")
    public ResponseEntity<ByteArrayResource> downloadFile() {
        // get the conected user
        User currentUser = getCurrentUser();

        // download the questions file from s3
        byte[] fileBytes = analyticsService.downloadQuestionsFile(currentUser);

        // wrap with Resource
        ByteArrayResource resource = new ByteArrayResource(fileBytes);

        // return as download file
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=unanswered_questions.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(fileBytes.length)
                .body(resource);
    }

    /**
     * Delete all the questions
     * It's happend when the webside owner press on "מחק שאלות" button
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearFile() {
        User currentUser = getCurrentUser();

        // delete the file
        analyticsService.deleteQuestionsFile(currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "השאלות נמחקו בהצלחה");

        return ResponseEntity.ok(response);
    }

    /**
     * Get the conected user
     * Inner help function
     */
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

    /**
     * ניתוח חכם של השאלות עם AI
     */
    @GetMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeQuestions() {
        User currentUser = getCurrentUser();

        log.info("📊 Starting AI analysis for user: {}", currentUser.getId());

        try {
            AnalysisResponse analysis = analyticsService.analyzeQuestions(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", analysis);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Analysis failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}