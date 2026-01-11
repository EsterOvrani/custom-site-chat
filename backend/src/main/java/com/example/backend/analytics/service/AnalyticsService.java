package com.example.backend.analytics.service;

import com.example.backend.analytics.dto.AnalysisResponse;
import com.example.backend.collection.service.CollectionService;
import com.example.backend.user.model.User;
import com.example.backend.common.infrastructure.storage.S3Service;
import com.example.backend.query.service.PromptService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final S3Service s3Service;
    private final CollectionService collectionService;
    private final OpenAiChatModel chatModel;
    private final PromptService promptService;

    // Get user by the key
    public User getUserBySecretKey(String secretKey) {
        return collectionService.validateSecretKey(secretKey);
    }

    // Append the questions to the questions file in S3
    public void appendQuestionsToFile(User user, List<String> newQuestions, String siteCategory) {
        String filePath = getFilePath(user);

        List<String> allQuestions = new ArrayList<>();

        // 1. Check if the file exists, if not read the content
        try {
            InputStream existing = s3Service.downloadFile(filePath);
            String content = new String(existing.readAllBytes(), StandardCharsets.UTF_8);

            // Extract the questions from the file
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                // Take the questions only without empty lines
                if (!line.isEmpty() && !line.startsWith("שאלה")) {
                    allQuestions.add(line);
                }
            }

            log.info("📖 Found {} existing questions in file", allQuestions.size());

        } catch (Exception e) {
            log.info("📝 No existing file found, will create new one");
        }

        // 2. Filter the questions with AI
        if (siteCategory != null && !siteCategory.trim().isEmpty()) {
            log.info("🔍 Filtering {} new questions for category: {}",
                    newQuestions.size(), siteCategory);

            List<String> filteredNew = filterWithLLM(newQuestions, siteCategory);
            allQuestions.addAll(filteredNew);

            log.info("✅ Added {} relevant questions (filtered from {} total)",
                    filteredNew.size(), newQuestions.size());
        } else {
            // If there is no category add the questions without filter
            allQuestions.addAll(newQuestions);
            log.info("ℹ️ No category provided - added all {} questions without filtering",
                    newQuestions.size());
        }

        // 3. Save all questions back to file
        saveQuestionsToFile(user, allQuestions);

        log.info("✅ Total questions in file now: {}", allQuestions.size());
    }

    // Filter the questions with AI
    private List<String> filterWithLLM(List<String> questions, String siteCategory) {
        log.info("🔍 Filtering {} questions with LLM for category: {}",
                questions.size(), siteCategory);

        // Build question list with numbers
        StringBuilder questionsText = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            questionsText.append((i + 1)).append(". ").append(questions.get(i)).append("\n");
        }

        // Get prompt from PromptService (includes system message in first line)
        String fullPrompt = promptService.getAnalyticsFilterPrompt(siteCategory, questionsText.toString());

        try {
            // Send to AI - using the prompt as-is (no separate system message needed)
            Response<AiMessage> response = chatModel.generate(
                    UserMessage.from(fullPrompt)
            );

            String answer = response.content().text().trim();
            log.info("📥 LLM response: {}", answer);

            // If there are no relevant questions
            if (answer.equalsIgnoreCase("NONE")) {
                log.info("ℹ️ LLM found no relevant questions");
                return new ArrayList<>();
            }

            // Answer parsing - converting numbers to questions
            List<String> filtered = new ArrayList<>();
            String[] indices = answer.split(",");

            for (String indexStr : indices) {
                try {
                    int index = Integer.parseInt(indexStr.trim()) - 1; // Because array starts from 0 index
                    if (index >= 0 && index < questions.size()) {
                        filtered.add(questions.get(index));
                    }
                } catch (NumberFormatException e) {
                    log.warn("⚠️ Could not parse index: {}", indexStr);
                }
            }

            log.info("✅ Filtered to {} relevant questions out of {}",
                    filtered.size(), questions.size());
            return filtered;

        } catch (Exception e) {
            log.error("❌ LLM filtering failed, returning all questions", e);
            return questions; // In error case, return all
        }
    }

    // Save the questions list to file
    private void saveQuestionsToFile(User user, List<String> questions) {
        String filePath = getFilePath(user);

        // Build file content
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            content.append("שאלה ").append(i + 1).append("\n");
            content.append(questions.get(i)).append("\n\n");
        }

        // Convert to bytes
        byte[] bytes = content.toString().getBytes(StandardCharsets.UTF_8);

        // Upload to S3
        s3Service.uploadFile(
                new ByteArrayInputStream(bytes),
                filePath,
                "text/plain; charset=UTF-8",
                bytes.length
        );

        log.info("💾 Saved {} questions to S3: {}", questions.size(), filePath);
    }

    // Download the questions file
    public byte[] downloadQuestionsFile(User user) {
        String filePath = getFilePath(user);

        try {
            InputStream inputStream = s3Service.downloadFile(filePath);
            byte[] fileBytes = inputStream.readAllBytes();
            
            // Check if file is empty or contains only whitespace
            if (fileBytes.length == 0) {
                log.warn("⚠️ Questions file is empty for user: {}", user.getId());
                throw new com.example.backend.common.exception.ResourceNotFoundException("קובץ השאלות ריק. אין שאלות להורדה.");
            }
            
            // Check if file contains actual questions (not just whitespace)
            String content = new String(fileBytes, StandardCharsets.UTF_8).trim();
            if (content.isEmpty()) {
                log.warn("⚠️ Questions file contains only whitespace for user: {}", user.getId());
                throw new com.example.backend.common.exception.ResourceNotFoundException("קובץ השאלות ריק. אין שאלות להורדה.");
            }
            
            return fileBytes;
        } catch (com.example.backend.common.exception.ResourceNotFoundException e) {
            // Re-throw ResourceNotFoundException as-is
            throw e;
        } catch (Exception e) {
            log.error("❌ File not found: {}", filePath);
            throw new com.example.backend.common.exception.ResourceNotFoundException("לא נמצאו שאלות. אנא נסה לאסוף שאלות תחילה.");
        }
    }

    // Delete questions file
    public void deleteQuestionsFile(User user) {
        String filePath = getFilePath(user);
        
        // Check if file exists before trying to delete
        if (!s3Service.fileExists(filePath)) {
            log.warn("⚠️ No questions file to delete for user: {}", user.getId());
            throw new com.example.backend.common.exception.ResourceNotFoundException("אין שאלות למחיקה. הקובץ לא קיים.");
        }
        
        try {
            s3Service.deleteFile(filePath);
            log.info("🗑️ Deleted questions file: {}", filePath);
        } catch (Exception e) {
            log.error("❌ Failed to delete file: {}", filePath, e);
            throw new RuntimeException("שגיאה במחיקת הקובץ: " + e.getMessage());
        }
    }

    // Get file path in S3
    private String getFilePath(User user) {
        return String.format("users/%d/analytics/questions.txt", user.getId());
    }

    /**
     * Analyze questions with AI
     * Groups questions by category, removes duplicates, and provides insights
     */
    public AnalysisResponse analyzeQuestions(User user) {
        String filePath = getFilePath(user);

        try {
            // 1. Check if questions file exists
            if (!s3Service.fileExists(filePath)) {
                log.warn("⚠️ No questions file found for user: {}", user.getId());
                throw new com.example.backend.common.exception.ResourceNotFoundException("לא נמצאו שאלות לניתוח.");
            }

            // 2. Download file from S3
            InputStream inputStream = s3Service.downloadFile(filePath);
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // 3. Extract questions from file content
            List<String> questions = new ArrayList<>();
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                // Skip empty lines and "Question N" headers
                if (!line.isEmpty() && !line.startsWith("שאלה")) {
                    questions.add(line);
                }
            }

            // 4. Check if there are actual questions to analyze
            if (questions.isEmpty()) {
                log.warn("⚠️ Questions file is empty for user: {}", user.getId());
                throw new com.example.backend.common.exception.ResourceNotFoundException("קובץ השאלות ריק.");
            }

            log.info("🔍 Analyzing {} questions with AI", questions.size());

            // 5. Build questions text for prompt
            StringBuilder questionsText = new StringBuilder();
            for (int i = 0; i < questions.size(); i++) {
                questionsText.append((i + 1)).append(". ").append(questions.get(i)).append("\n");
            }

            // 6. Get prompt from PromptService (includes system message in first line)
            String fullPrompt = promptService.getAnalyticsAnalysisPrompt(questionsText.toString());

            // 7. Send to AI for analysis - using the prompt as-is (no separate system message needed)
            Response<AiMessage> response = chatModel.generate(
                    UserMessage.from(fullPrompt)
            );

            String aiResponse = response.content().text().trim();
            log.info("📥 AI Response received: {}", aiResponse);

            // 8. Clean response - remove markdown backticks if present
            aiResponse = aiResponse
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .trim();

            // 9. Parse JSON response to AnalysisResponse object
            ObjectMapper mapper = new ObjectMapper();
            AnalysisResponse analysis = mapper.readValue(aiResponse, AnalysisResponse.class);

            log.info("✅ Analysis completed: {} categories found", analysis.getCategories().size());
            return analysis;

        } catch (com.example.backend.common.exception.ResourceNotFoundException e) {
            // Re-throw ResourceNotFoundException as-is
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to analyze questions", e);
            throw new RuntimeException("נכשל בניתוח השאלות: " + e.getMessage());
        }
    }
}