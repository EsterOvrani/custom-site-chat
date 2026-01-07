package com.example.backend.analytics.service;

import com.example.backend.collection.service.CollectionService;
import com.example.backend.user.model.User;
import com.example.backend.common.infrastructure.storage.S3Service;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;

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

    // get user by the key
    public User getUserBySecretKey(String secretKey) {
        return collectionService.validateSecretKey(secretKey);
    }

    // append the unquestion to the question s file in S3
    public void appendQuestionsToFile(User user, List<String> newQuestions, String siteCategory) {
        String filePath = getFilePath(user);
        
        List<String> allQuestions = new ArrayList<>();
        
        // 1. check if the file is empty if mot read the contex
        try {
            InputStream existing = s3Service.downloadFile(filePath);
            String content = new String(existing.readAllBytes(), StandardCharsets.UTF_8);
            
            // extract the questions from the file
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                // take the questions only without empty line...
                if (!line.isEmpty() && !line.startsWith("שאלה")) {
                    allQuestions.add(line);
                }
            }
            
            log.info("📖 Found {} existing questions in file", allQuestions.size());
            
        } catch (Exception e) {
            log.info("📝 No existing file found, will create new one");
        }
        
        // 2. filter the question with AI
        if (siteCategory != null && !siteCategory.trim().isEmpty()) {
            log.info("🔍 Filtering {} new questions for category: {}", 
                newQuestions.size(), siteCategory);
            
            List<String> filteredNew = filterWithLLM(newQuestions, siteCategory);
            allQuestions.addAll(filteredNew);
            
            log.info("✅ Added {} relevant questions (filtered from {} total)", 
                filteredNew.size(), newQuestions.size());
        } else {
            // if the is no category add the question without filter
            allQuestions.addAll(newQuestions);
            log.info("ℹ️ No category provided - added all {} questions without filtering", 
                newQuestions.size());
        }
        
        // 3. save the all questions backed to file
        saveQuestionsToFile(user, allQuestions);
        
        log.info("✅ Total questions in file now: {}", allQuestions.size());
    }

    // filter the question with AI
    private List<String> filterWithLLM(List<String> questions, String siteCategory) {
        log.info("🔍 Filtering {} questions with LLM for category: {}", 
            questions.size(), siteCategory);

        // build question list with numbers
        StringBuilder questionsText = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            questionsText.append((i + 1)).append(". ").append(questions.get(i)).append("\n");
        }

        // prompt to AI
        String prompt = String.format("""
            אתה מסנן שאלות לפי רלוונטיות לאתר.
            
            נושא האתר: %s
            
            השאלות הבאות נשאלו על ידי לקוחות:
            %s
            
            החזר רק את המספרים של השאלות שרלוונטיות לנושא האתר.
            אל תכלול: בדיחות, שאלות כלליות שלא קשורות לנושא, דברים אישיים.
            
            פורמט תשובה: מספרים מופרדים בפסיקים בלבד (לדוגמה: 1,3,5,7)
            אם אין שאלות רלוונטיות בכלל, החזר: NONE
            """, 
            siteCategory,
            questionsText
        );

        try {
            // send to AI
            Response<AiMessage> response = chatModel.generate(
                SystemMessage.from("אתה מומחה לסינון שאלות לפי רלוונטיות."),
                UserMessage.from(prompt)
            );

            String answer = response.content().text().trim();
            log.info("📥 LLM response: {}", answer);

            // id there is no relevant questions
            if (answer.equalsIgnoreCase("NONE")) {
                log.info("ℹ️ LLM found no relevant questions");
                return new ArrayList<>();
            }

            // Answer press - converting numbers to questions
            List<String> filtered = new ArrayList<>();
            String[] indices = answer.split(",");
            
            for (String indexStr : indices) {
                try {
                    int index = Integer.parseInt(indexStr.trim()) - 1; // cuz array start from 0 index
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
            return questions; // in error case, return all
        }
    }

    // the the questions list to file
    private void saveQuestionsToFile(User user, List<String> questions) {
        String filePath = getFilePath(user);
        
        // build context file
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            content.append("שאלה ").append(i + 1).append("\n");
            content.append(questions.get(i)).append("\n\n");
        }
        
        // convert to bytes
        byte[] bytes = content.toString().getBytes(StandardCharsets.UTF_8);
        
        // upload to s3
        s3Service.uploadFile(
            new ByteArrayInputStream(bytes),
            filePath,
            "text/plain; charset=UTF-8",
            bytes.length
        );
        
        log.info("💾 Saved {} questions to S3: {}", questions.size(), filePath);
    }

    // download the questions file
    public byte[] downloadQuestionsFile(User user) {
        String filePath = getFilePath(user);
        
        try {
            InputStream inputStream = s3Service.downloadFile(filePath);
            return inputStream.readAllBytes();
        } catch (Exception e) {
            log.error("❌ File not found: {}", filePath);
            throw new RuntimeException("לא נמצא קובץ שאלות");
        }
    }

    // delete questions file
    public void deleteQuestionsFile(User user) {
        String filePath = getFilePath(user);
        try {
            s3Service.deleteFile(filePath);
            log.info("🗑️ Deleted questions file: {}", filePath);
        } catch (Exception e) {
            log.error("❌ Failed to delete file: {}", filePath, e);
        }
    }

    // get file path in S3
    private String getFilePath(User user) {
        return String.format("users/%d/analytics/questions.txt", user.getId());
    }
}