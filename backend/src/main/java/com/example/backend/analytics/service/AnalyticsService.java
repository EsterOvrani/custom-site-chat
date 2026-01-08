package com.example.backend.analytics.service;

import com.example.backend.analytics.dto.AnalysisResponse;
import com.example.backend.collection.service.CollectionService;
import com.example.backend.user.model.User;
import com.example.backend.common.infrastructure.storage.S3Service;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

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

    // delete questions file
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

    // get file path in S3
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

            // 5. Build prompt for AI analysis
            String prompt = buildAnalysisPrompt(questions);

            // 6. Send to AI for analysis
            Response<AiMessage> response = chatModel.generate(
                    SystemMessage.from("אתה מומחה לניתוח וסיווג שאלות לקוחות. תחזיר תשובה במבנה JSON בלבד."),
                    UserMessage.from(prompt)
            );

            String aiResponse = response.content().text().trim();
            log.info("📥 AI Response received: {}", aiResponse);

            // 7. Clean response - remove markdown backticks if present
            aiResponse = aiResponse
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .trim();

            // 8. Parse JSON response to AnalysisResponse object
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

    // בניית prompt מובנה
    private String buildAnalysisPrompt(List<String> questions) {
        StringBuilder questionsText = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            questionsText.append((i + 1)).append(". ").append(questions.get(i)).append("\n");
        }

        return String.format("""
            נתונות השאלות הבאות מלקוחות:
            %s
            
            המשימה שלך: לאחד שאלות לפי הכוונה/משמעות שלהן, לא רק לפי ניסוח זהה.
            
            כללי איחוד שאלות (חשוב מאוד!):
            - שאלות עם אותה כוונה = אותה שאלה, גם אם המילים שונות
            - "כמה עולה משלוח?" = "מה מחיר המשלוח?" = "יש עלות על משלוח?" (כולן שאלה אחת!)
            - "אפשר להחזיר?" = "מה מדיניות ההחזרות?" = "איך מחזירים מוצר?" (כולן שאלה אחת!)
            - "מתי מגיעה החבילה?" = "כמה זמן לוקח משלוח?" = "מה זמן האספקה?" (כולן שאלה אחת!)
            - "יש לכם במידה X?" = "באיזה מידות יש?" = "מה המידות הזמינות?" (כולן שאלה אחת!)
            
            דוגמה לקלט:
            1. כמה עולה משלוח?
            2. מה מחיר המשלוח?
            3. יש משלוח חינם?
            4. האם יש עלות למשלוח?
            
            דוגמה לפלט נכון:
            - "כמה עולה משלוח?" (count: 3) - מאחד שאלות 1,2,4
            - "האם יש משלוח חינם?" (count: 1) - שאלה 3 נפרדת כי הכוונה שונה
            
            בצע ניתוח:
            1. קרא כל שאלה והבן את הכוונה האמיתית שלה
            2. אחד שאלות עם כוונה זהה (גם אם ניסוח שונה לגמרי)
            3. בחר ניסוח מייצג ברור לכל קבוצת שאלות
            4. קבץ לקטגוריות לפי נושא
            5. ספור כמה שאלות מקוריות נכללו בכל שאלה מאוחדת
            
            החזר JSON בפורמט הזה בדיוק:
            {
            "categories": [
                {
                "categoryName": "שם הקטגוריה",
                "icon": "אייקון אימוג'י",
                "questions": [
                    {
                    "question": "השאלה המייצגת",
                    "count": 5
                    }
                ],
                "totalCount": 5
                }
            ],
            "summary": "סיכום קצר של הממצאים - ציין כמה שאלות מקוריות אוחדו",
            "totalQuestions": 10
            }
            
            כללים חשובים:
            - החזר רק JSON, ללא טקסט נוסף
            - אל תוסיף מרכאות או backticks מסביב ל-JSON
            - אייקון מתאים לכל קטגוריה (📦 משלוחים, 💰 מחירים, 📏 מידות, 🔄 החזרות...)
            - totalQuestions = סה"כ השאלות המקוריות (לא אחרי איחוד)
            - count בכל שאלה = כמה שאלות מקוריות אוחדו לשאלה זו
            - היה אגרסיבי באיחוד! אם יש ספק - אחד!
            """, questionsText);
    }

}