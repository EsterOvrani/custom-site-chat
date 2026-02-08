package com.example.backend.common.infrastructure.tokenizer;

import com.example.backend.user.model.User;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.common.exception.ResourceNotFoundException;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * שירות מרכזי לניהול צריכת טוקנים
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenUsageService {
    
    private final UserRepository userRepository;
    private final OpenAiTokenizer tokenizer = new OpenAiTokenizer("gpt-4o");
    
    /**
     * עדכן שימוש בטוקנים למשתמש
     */
    public void addTokenUsage(Long userId, int tokens, String context) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", userId));
            
            long beforeUsage = user.getTokensUsed();
            user.addTokenUsage(tokens);
            userRepository.save(user);
            
            log.info("💰 [{}] {} - Added {} tokens. Total: {} / {} ({}%)",
                userId,
                context,
                tokens,
                user.getTokensUsed(),
                user.getTokenQuota(),
                String.format("%.1f", user.getUsagePercentage()));
                
            // אזהרה אם מתקרבים לגבול
            if (user.getUsagePercentage() >= 80 && beforeUsage < user.getTokenQuota() * 0.8) {
                log.warn("⚠️ [{}] User approaching token limit! {}%", 
                    userId, String.format("%.1f", user.getUsagePercentage()));
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to update token usage for user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * חישוב טוקנים מרשימת הודעות
     */
    public int calculateTokensForMessages(List<ChatMessage> messages) {
        return tokenizer.estimateTokenCountInMessages(messages);
    }
    
    /**
     * חישוב טוקנים מהודעה בודדת
     */
    public int calculateTokensForMessage(AiMessage message) {
        return tokenizer.estimateTokenCountInMessage(message);
    }
    
    /**
     * חישוב טוקנים מטקסט
     */
    public int calculateTokensForText(String text) {
        return tokenizer.estimateTokenCountInText(text);
    }
    
    /**
     * בדיקה אם יש מספיק טוקנים
     */
    public boolean checkTokenQuota(Long userId, int requiredTokens) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", userId));
            
            return user.hasEnoughTokens(requiredTokens);
            
        } catch (Exception e) {
            log.error("Failed to check token quota", e);
            return true; // במקרה של שגיאה, אפשר להמשיך
        }
    }
}
