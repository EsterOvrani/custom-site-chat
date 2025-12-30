package com.example.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionEndedRequest {

    private String secretKey;
    private List<ConversationMessage> conversationHistory;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationMessage {
        private String role;        // "user" or "assistant"
        private String content;     // תוכן ההודעה
    }
}