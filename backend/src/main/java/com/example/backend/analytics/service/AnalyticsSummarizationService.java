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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Summarization Service - OpenAI integration
 * Uses GPT-4 to consolidate duplicate questions and categories
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsSummarizationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url}")
    private String openAiApiUrl;

    // ========================================================================
    // SUMMARIZE QUESTIONS
    // ========================================================================

    /**
     * Consolidate duplicate/similar questions using OpenAI
     */
    public List<QuestionSummary> summarizeQuestions(List<String> rawQuestions) {
        try {
            log.info("🔄 Summarizing {} questions with OpenAI...", rawQuestions.size());

            if (rawQuestions.isEmpty()) {
                return List.of();
            }

            // Build prompt
            String prompt = buildQuestionSummarizationPrompt(rawQuestions);

            // Call OpenAI
            String response = callOpenAI(prompt);

            // Parse response
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
        
        sb.append("נתונה רשימת שאלות שמשתמשים שאלו בצ'אט. חלקן כפולות או דומות.\n\n");
        sb.append("רשימת השאלות (סה\"כ ").append(questions.size()).append("):\n");
        for (int i = 0; i < questions.size(); i++) {
            sb.append((i + 1)).append(". ").append(questions.get(i)).append("\n");
        }
        
        sb.append("\nמשימה:\n");
        sb.append("1. זהה שאלות דומות/כפולות (לא בהכרח אותו ניסוח מדויק)\n");
        sb.append("2. קבץ אותן לשאלה אחת מייצגת (הניסוח הכי ברור)\n");
        sb.append("3. ספור כמה פעמים כל שאלה נשאלה (כולל כל הוריאציות)\n");
        sb.append("4. תן עד 3 דוגמאות לניסוחים שונים\n\n");
        
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
        sb.append("- השאלה המייצגת צריכה להיות הניסוח הכי ברור ומלא\n");
        sb.append("- ספור נכון את כל הוריאציות (כולל שאלות קצרות/ארוכות)\n");
        sb.append("- אם יש שאלות ייחודיות לגמרי, החזר אותן בנפרד עם count: 1\n\n");
        
        sb.append("החזר JSON בפורמט:\n");
        sb.append("[\n");
        sb.append("  { \"question\": \"שאלה מנורמלת\", \"count\": מספר, \"examples\": [\"דוגמה 1\", \"דוגמה 2\"] }\n");
        sb.append("]");
        
        return sb.toString();
    }

    private List<QuestionSummary> parseQuestionSummaries(String jsonResponse) throws Exception {
        // Remove markdown code blocks if present
        String cleaned = jsonResponse.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("```json", "").replaceFirst("```", "").trim();
        }

        // Parse JSON
        return objectMapper.readValue(cleaned, new TypeReference<List<QuestionSummary>>() {});
    }

    // ========================================================================
    // SUMMARIZE CATEGORIES
    // ========================================================================

    /**
     * Consolidate duplicate/similar categories using OpenAI
     */
    public List<CategoryStats> summarizeCategories(List<String> rawCategories) {
        try {
            log.info("🔄 Summarizing {} categories with OpenAI...", rawCategories.size());

            if (rawCategories.isEmpty()) {
                return List.of();
            }

            // Build prompt
            String prompt = buildCategorySummarizationPrompt(rawCategories);

            // Call OpenAI
            String response = callOpenAI(prompt);

            // Parse response
            List<CategoryStats> stats = parseCategoryStats(response);

            // Calculate percentages
            int total = stats.stream().mapToInt(CategoryStats::getCount).sum();
            for (CategoryStats stat : stats) {
                double percentage = (double) stat.getCount() / total * 100;
                stat.setPercentage(percentage);
            }

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
        
        sb.append("Consolidate duplicate Hebrew category names and count their occurrences.\n\n");
        
        sb.append("Categories (").append(categories.size()).append(" total):\n");
        for (int i = 0; i < categories.size(); i++) {
            sb.append("- ").append(categories.get(i)).append("\n");
        }
        
        sb.append("\nInstructions:\n");
        sb.append("1. Identify duplicate/similar categories (e.g., \"מחירים\" and \"מחיר\" are the same)\n");
        sb.append("2. Consolidate into ONE representative name per unique category\n");
        sb.append("3. Count total occurrences including all variations\n\n");
        
        sb.append("Return a JSON object with this structure:\n");
        sb.append("{\n");
        sb.append("  \"categories\": [\n");
        sb.append("    { \"category\": \"name\", \"count\": number },\n");
        sb.append("    ...\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        
        sb.append("Example:\n");
        sb.append("Input: [\"מחירים\", \"מחיר\", \"משלוחים\", \"משלוח\"]\n");
        sb.append("Output:\n");
        sb.append("{\n");
        sb.append("  \"categories\": [\n");
        sb.append("    { \"category\": \"מחירים\", \"count\": 2 },\n");
        sb.append("    { \"category\": \"משלוחים\", \"count\": 2 }\n");
        sb.append("  ]\n");
        sb.append("}");
        
        return sb.toString();
    }

    private List<CategoryStats> parseCategoryStats(String jsonResponse) throws Exception {
        // Clean response
        String cleaned = jsonResponse.trim();
        
        log.info("Raw OpenAI response: {}", cleaned);
        
        // Remove markdown code blocks if present
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("```json\\s*", "")
                           .replaceFirst("```\\s*$", "")
                           .trim();
        }
        
        // Find the JSON object
        int objectStart = cleaned.indexOf('{');
        if (objectStart > 0) {
            cleaned = cleaned.substring(objectStart);
        }
        
        int objectEnd = cleaned.lastIndexOf('}');
        if (objectEnd > 0 && objectEnd < cleaned.length() - 1) {
            cleaned = cleaned.substring(0, objectEnd + 1);
        }
        
        log.info("Cleaned JSON: {}", cleaned);

        // Parse JSON object
        Map<String, Object> wrapper = objectMapper.readValue(cleaned, 
            new TypeReference<Map<String, Object>>() {});
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> categoriesData = (List<Map<String, Object>>) wrapper.get("categories");
        
        // Convert to CategoryStats
        List<CategoryStats> stats = new ArrayList<>();
        for (Map<String, Object> data : categoriesData) {
            CategoryStats stat = new CategoryStats();
            stat.setCategory((String) data.get("category"));
            stat.setCount(((Number) data.get("count")).intValue());
            stat.setPercentage(0.0); // Will be calculated after
            stats.add(stat);
        }
        
        return stats;
    }

    // ========================================================================
    // OPENAI API CALL
    // ========================================================================

    private String callOpenAI(String prompt) {
        try {
            // Build request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 2000);
            requestBody.put("response_format", Map.of("type", "json_object"));

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Call API
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                openAiApiUrl,
                request,
                Map.class
            );

            // Extract content
            if (response == null || !response.containsKey("choices")) {
                throw new RuntimeException("Invalid OpenAI response");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            
            if (choices.isEmpty()) {
                throw new RuntimeException("No choices in OpenAI response");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            
            String content = (String) message.get("content");
            
            log.debug("OpenAI response: {}", content);
            
            return content;

        } catch (Exception e) {
            log.error("❌ OpenAI API call failed", e);
            throw new RuntimeException("נכשל בקריאה ל-OpenAI API", e);
        }
    }
}