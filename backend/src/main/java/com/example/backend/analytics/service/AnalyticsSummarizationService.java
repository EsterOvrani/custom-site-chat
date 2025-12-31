package com.example.backend.analytics.service;

import com.example.backend.analytics.dto.CategoryStats;
import com.example.backend.analytics.dto.QuestionSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Analytics Summarization Service
 * 
 * Uses OpenAI to consolidate duplicate questions and categories
 * This runs ONLY when user requests a report (not on every session)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsSummarizationService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${openai.api-key}")
    private String openAiApiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4";

    // ========================================================================
    // SUMMARIZE QUESTIONS
    // ========================================================================

    /**
     * Consolidate duplicate questions
     * Input: ["כמה עולה החולצה?", "כמה עולה", "מחיר חולצה"]
     * Output: [{ question: "כמה עולה החולצה?", count: 3, examples: [...] }]
     */
    public List<QuestionSummary> summarizeQuestions(List<String> rawQuestions) {
        try {
            log.info("🔄 Summarizing {} questions with OpenAI...", rawQuestions.size());

            String prompt = buildQuestionSummarizationPrompt(rawQuestions);
            String response = callOpenAI(prompt);
            
            List<QuestionSummary> summaries = parseQuestionSummaries(response);
            
            log.info("✅ Summarized {} raw questions → {} unique questions", 
                rawQuestions.size(), summaries.size());
            
            return summaries;

        } catch (Exception e) {
            log.error("❌ Failed to summarize questions", e);
            throw new RuntimeException("נכשל בסיכום שאלות עם OpenAI", e);
        }
    }

    private String buildQuestionSummarizationPrompt(List<String> questions) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("נתונה רשימת שאלות שמשתמשים שאלו. חלקן כפולות או דומות.\n\n");
        sb.append("רשימת השאלות:\n");
        for (int i = 0; i < questions.size(); i++) {
            sb.append((i + 1)).append(". ").append(questions.get(i)).append("\n");
        }
        
        sb.append("\nמשימה:\n");
        sb.append("1. זהה שאלות דומות/כפולות\n");
        sb.append("2. קבץ אותן לשאלה אחת מייצגת\n");
        sb.append("3. ספור כמה פעמים כל שאלה נשאלה (כולל וריאציות)\n");
        sb.append("4. תן דוגמאות לניסוחים שונים (עד 3 דוגמאות)\n\n");
        
        sb.append("דוגמה:\n");
        sb.append("קלט: [\"כמה עולה חולצה?\", \"מה המחיר של החולצה\", \"כמה עולה\"]\n");
        sb.append("פלט:\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"question\": \"כמה עולה החולצה?\",\n");
        sb.append("    \"count\": 3,\n");
        sb.append("    \"examples\": [\"כמה עולה חולצה?\", \"מה המחיר של החולצה\", \"כמה עולה\"]\n");
        sb.append("  }\n");
        sb.append("]\n\n");
        
        sb.append("חשוב:\n");
        sb.append("- החזר JSON בלבד (ללא backticks או טקסט נוסף)\n");
        sb.append("- השאלה המייצגת צריכה להיות הניסוח הכי ברור\n");
        sb.append("- ספור נכון את כל הוריאציות\n\n");
        
        sb.append("החזר JSON בפורמט:\n");
        sb.append("[\n");
        sb.append("  { \"question\": \"שאלה מנורמלת\", \"count\": מספר, \"examples\": [\"דוגמה 1\", \"דוגמה 2\"] }\n");
        sb.append("]");
        
        return sb.toString();
    }

    private List<QuestionSummary> parseQuestionSummaries(String response) throws Exception {
        String cleaned = cleanJsonResponse(response);
        return objectMapper.readValue(cleaned, new TypeReference<List<QuestionSummary>>() {});
    }

    // ========================================================================
    // SUMMARIZE CATEGORIES
    // ========================================================================

    /**
     * Consolidate duplicate categories
     * Input: ["מחירים", "מחיר", "עלות", "משלוחים", "משלוח"]
     * Output: [{ category: "מחירים", count: 3, percentage: 60 }, ...]
     */
    public List<CategoryStats> summarizeCategories(List<String> rawCategories) {
        try {
            log.info("🔄 Summarizing {} categories with OpenAI...", rawCategories.size());

            String prompt = buildCategorySummarizationPrompt(rawCategories);
            String response = callOpenAI(prompt);
            
            List<CategoryStats> stats = parseCategoryStats(response);
            
            log.info("✅ Summarized {} raw categories → {} unique categories", 
                rawCategories.size(), stats.size());
            
            return stats;

        } catch (Exception e) {
            log.error("❌ Failed to summarize categories", e);
            throw new RuntimeException("נכשל בסיכום קטגוריות עם OpenAI", e);
        }
    }

    private String buildCategorySummarizationPrompt(List<String> categories) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("נתונה רשימת נושאים/קטגוריות ממספר שיחות. חלקן כפולות או דומות.\n\n");
        sb.append("רשימת הקטגוריות:\n");
        for (int i = 0; i < categories.size(); i++) {
            sb.append((i + 1)).append(". ").append(categories.get(i)).append("\n");
        }
        
        sb.append("\nמשימה:\n");
        sb.append("1. קבץ קטגוריות דומות יחד\n");
        sb.append("2. ספור כמה פעמים כל קטגוריה הופיעה\n");
        sb.append("3. חשב אחוזים (count / total * 100)\n");
        sb.append("4. החזר עד 10 קטגוריות (הכי תכופות)\n\n");
        
        sb.append("דוגמה:\n");
        sb.append("קלט: [\"מחירים\", \"מחיר\", \"עלות\", \"משלוחים\", \"משלוח\"]\n");
        sb.append("פלט:\n");
        sb.append("[\n");
        sb.append("  { \"category\": \"מחירים\", \"count\": 3, \"percentage\": 60.0 },\n");
        sb.append("  { \"category\": \"משלוחים\", \"count\": 2, \"percentage\": 40.0 }\n");
        sb.append("]\n\n");
        
        sb.append("חשוב:\n");
        sb.append("- החזר JSON בלבד\n");
        sb.append("- סכום האחוזים צריך להיות 100\n");
        sb.append("- קטגוריה = 1-2 מילים כלליות\n\n");
        
        sb.append("החזר JSON בפורמט:\n");
        sb.append("[\n");
        sb.append("  { \"category\": \"שם קטגוריה\", \"count\": מספר, \"percentage\": אחוז }\n");
        sb.append("]");
        
        return sb.toString();
    }

    private List<CategoryStats> parseCategoryStats(String response) throws Exception {
        String cleaned = cleanJsonResponse(response);
        return objectMapper.readValue(cleaned, new TypeReference<List<CategoryStats>>() {});
    }

    // ========================================================================
    // OPENAI API CALL
    // ========================================================================

    private String callOpenAI(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.3);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            OPENAI_URL,
            request,
            Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("Empty response from OpenAI");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No choices in OpenAI response");
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private String cleanJsonResponse(String response) {
        // Remove markdown code blocks if present
        return response
            .replaceAll("```json\\s*", "")
            .replaceAll("```\\s*", "")
            .trim();
    }
}