package com.example.backend.query.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicQueryRequest {
    
    @NotBlank(message = "Secret key is required")
    private String secretKey;
    
    @NotBlank(message = "Question is required")
    @Size(min = 1, max = 2000)
    private String question;
    
    // 🆕 היסטוריה מהלקוח (מקסימום 10 הודעות)
    private List<HistoryMessage> history;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryMessage {
        private String role;     // "user" או "assistant"
        private String content;  // הטקסט (לא ווקטור!)
        
        public boolean isUser() {
            return "user".equalsIgnoreCase(role);
        }
        
        public boolean isAssistant() {
            return "assistant".equalsIgnoreCase(role);
        }
    }
}