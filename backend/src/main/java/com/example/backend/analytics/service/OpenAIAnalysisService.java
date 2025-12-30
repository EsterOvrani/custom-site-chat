package com.example.backend.analytics.service;

import com.example.backend.analytics.dto.CategoryStats;
import com.example.backend.analytics.dto.QuestionSummary;
import com.example.backend.analytics.dto.SessionEndedRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIAnalysisService {
    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    public List<String> extractUnansweredQuestions(List<SessionEndedRequest.ConversationMessage> conversation, String businessType) {
        try {
            log.info("🔍 Analyzing conversation for unanswered questions...");
            String prompt = buildUnansweredQuestionsPrompt(conversation, businessType);
            String response = chatModel.generate(prompt);
            String cleanJson = cleanJsonResponse(response);
            List<String> questions = objectMapper.readValue(cleanJson, new TypeReference<List<String>>() {});
            log.info("✅ Extracted {} unanswered questions", questions.size());
            return questions;
        } catch (Exception e) {
            log.error("❌ Failed to extract unanswered questions", e);
            return new ArrayList<>();
        }
    }

    public List<String> extractTopics(List<SessionEndedRequest.ConversationMessage> conversation, String businessType) {
        try {
            log.info("🔍 Analyzing conversation for topics...");
            String prompt = buildTopicsPrompt(conversation, businessType);
            String response = chatModel.generate(prompt);
            String cleanJson = cleanJsonResponse(response);
            List<String> topics = objectMapper.readValue(cleanJson, new TypeReference<List<String>>() {});
            log.info("✅ Extracted {} topics", topics.size());
            return topics;
        } catch (Exception e) {
            log.error("❌ Failed to extract topics", e);
            return new ArrayList<>();
        }
    }

    public List<QuestionSummary> summarizeQuestions(List<String> rawQuestions, String businessType) {
        try {
            log.info("📊 Summarizing {} raw questions...", rawQuestions.size());
            String prompt = buildSummarizeQuestionsPrompt(rawQuestions, businessType);
            String response = chatModel.generate(prompt);
            String cleanJson = cleanJsonResponse(response);
            List<QuestionSummary> summaries = objectMapper.readValue(cleanJson, new TypeReference<List<QuestionSummary>>() {});
            log.info("✅ Summarized to {} unique questions", summaries.size());
            return summaries;
        } catch (Exception e) {
            log.error("❌ Failed to summarize questions", e);
            return new ArrayList<>();
        }
    }

    public List<CategoryStats> summarizeCategories(List<String> rawCategories, String businessType) {
        try {
            log.info("📊 Summarizing {} raw categories...", rawCategories.size());
            String prompt = buildSummarizeCategoriesPrompt(rawCategories, businessType);
            String response = chatModel.generate(prompt);
            String cleanJson = cleanJsonResponse(response);
            List<CategoryStats> stats = objectMapper.readValue(cleanJson, new TypeReference<List<CategoryStats>>() {});
            log.info("✅ Summarized to {} main categories", stats.size());
            return stats;
        } catch (Exception e) {
            log.error("❌ Failed to summarize categories", e);
            return new ArrayList<>();
        }
    }

    private String buildUnansweredQuestionsPrompt(List<SessionEndedRequest.ConversationMessage> conversation, String businessType) {
        StringBuilder sb = new StringBuilder();
        sb.append("אתה צ'אט בוט לאתר ").append(businessType).append(".\n\nהיסטוריית השיחה:\n");
        for (SessionEndedRequest.ConversationMessage msg : conversation) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        sb.append("\nמשימה:\n1. זהה אילו שאלות המשתמש שאלת ולא ידעת לענות עליהן\n");
        sb.append("2. כלול רק שאלות שקשורות לאתר (התעלם מבדיחות, שיחת חולין)\n");
        sb.append("3. אם שאלה היא שאלת המשך - נסח אותה מחדש כך שתהיה מובנת לבד\n\n");
        sb.append("דוגמאות:\n❌ \"ספר לי בדיחה\" → התעלם\n❌ \"מה המצב?\" → התעלם\n");
        sb.append("✅ \"ומה בבגדי ים?\" → \"האם אפשר להחזיר בגדי ים?\"\n");
        sb.append("✅ \"כמה זמן לאילת?\" → \"כמה זמן לוקח משלוח לאילת?\"\n\n");
        sb.append("החזר JSON בלבד (ללא backticks):\n[\"שאלה מנורמלת 1\", \"שאלה מנורמלת 2\"]");
        return sb.toString();
    }

    private String buildTopicsPrompt(List<SessionEndedRequest.ConversationMessage> conversation, String businessType) {
        StringBuilder sb = new StringBuilder();
        sb.append("אתה צ'אט בוט לאתר ").append(businessType).append(".\n\nהיסטוריית השיחה:\n");
        for (SessionEndedRequest.ConversationMessage msg : conversation) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        sb.append("\nמשימה:\nזהה את הנושאים/קטגוריות שהמשתמש התעניין בהם.\n\n");
        sb.append("חוקים:\n- כלול רק נושאים שקשורים לאתר\n- התעלם מ: בדיחות, שיחת חולין, שאלות כלליות\n");
        sb.append("- החזר עד 3 נושאים (הכי רלוונטיים)\n- שם קטגוריה: 1-2 מילים בעברית\n\n");
        sb.append("דוגמאות:\n✅ \"משלוחים\"\n✅ \"החזרות\"\n✅ \"מידע על מוצרים\"\n❌ \"כללי\"\n❌ \"שונות\"\n\n");
        sb.append("החזר JSON בלבד (ללא backticks):\n[\"קטגוריה 1\", \"קטגוריה 2\"]");
        return sb.toString();
    }

    private String buildSummarizeQuestionsPrompt(List<String> rawQuestions, String businessType) {
        StringBuilder sb = new StringBuilder();
        sb.append("יש לך רשימת שאלות גולמית שמשתמשים שאלו באתר ").append(businessType).append(":\n\n");
        for (String q : rawQuestions) {
            sb.append("- ").append(q).append("\n");
        }
        sb.append("\nמשימה:\n1. אחד שאלות שמשמעותן זהה (גם אם המילים שונות)\n");
        sb.append("2. ספור כמה פעמים כל שאלה נשאלה\n3. מיין מהכי נפוץ לפחות נפוץ\n\n");
        sb.append("דוגמאות לאיחוד:\n\"האם אפשר להחזיר?\"\n\"מה התהליך של החזרה?\"\n\"Can I return items?\"\n");
        sb.append("→ כולן = \"האם אפשר להחזיר מוצרים?\" (×3)\n\n");
        sb.append("החזר JSON בלבד (ללא backticks):\n[\n  {\n    \"question\": \"שאלה מנורמלת\",\n");
        sb.append("    \"count\": 8,\n    \"examples\": [\"דוגמה 1\", \"דוגמה 2\"]\n  }\n]");
        return sb.toString();
    }

    private String buildSummarizeCategoriesPrompt(List<String> rawCategories, String businessType) {
        StringBuilder sb = new StringBuilder();
        sb.append("יש לך רשימת נושאים גולמית מאתר ").append(businessType).append(":\n\n");
        for (String c : rawCategories) {
            sb.append("- ").append(c).append("\n");
        }
        sb.append("\nמשימה:\n1. אחד נושאים דומים לקטגוריות מרכזיות\n2. ספור כמה פעמים כל נושא הופיע\n");
        sb.append("3. חשב אחוזים\n4. מיין מהכי פופולרי לפחות\n\n");
        sb.append("דוגמאות לאיחוד:\n\"משלוחים\" + \"זמן משלוח\" + \"Delivery\" \n→ \"משלוחים\" (×12)\n\n");
        sb.append("\"החזרות\" + \"החזר כסף\" + \"Return policy\"\n→ \"החזרות\" (×8)\n\n");
        sb.append("חוק: לא יותר מ-10 קטגוריות מרכזיות.\nאם יש נושא עם <5% → כלול ב\"אחר\"\n\n");
        sb.append("החזר JSON בלבד (ללא backticks):\n[\n  {\n    \"category\": \"משלוחים\",\n");
        sb.append("    \"count\": 12,\n    \"percentage\": 35.5\n  }\n]");
        return sb.toString();
    }

    private String cleanJsonResponse(String response) {
        if (response == null) return "[]";
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        return cleaned.trim();
    }
}