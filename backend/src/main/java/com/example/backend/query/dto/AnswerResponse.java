package com.example.backend.query.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO לתשובה מה-AI
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerResponse {

    /**
     * התשובה מה-AI
     */
    private String answer;

    /**
     * האם התשובה הצליחה?
     */
    @Builder.Default
    private Boolean success = true;

    /**
     * רמת ביטחון בתשובה (0.0 - 1.0)
     */
    private Double confidence;

    /**
     * מקורות המידע
     */
    private List<Source> sources;

    /**
     * מזהה ההודעה שנשמרה (לא בשימוש יותר - אין שמירת הודעות)
     */
    private Long messageId;

    /**
     * כמה tokens השתמשנו
     */
    private Integer tokensUsed;

    /**
     * זמן תגובה במילישניות
     */
    private Long responseTimeMs;

    /**
     * מתי התשובה נוצרה
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * הודעת שגיאה (אם נכשל)
     */
    private String errorMessage;

    /**
     * הצעות למשך (אם התשובה לא ברורה)
     */
    private List<String> suggestions;

    // ==================== Inner Classes ====================

    /**
     * מידע על מקור
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        /**
         * מזהה המסמך
         */
        private Long documentId;

        /**
         * שם המסמך
         */
        private String documentName;

        /**
         * עמוד במסמך (אם ידוע)
         */
        private Integer pageNumber;

        /**
         * טקסט רלוונטי מהמסמך
         */
        private String excerpt;

        /**
         * רמת רלוונטיות (0.0 - 1.0)
         */
        private Double relevanceScore;

        /**
         * האם זה המקור העיקרי?
         */
        @Builder.Default
        private Boolean isPrimary = false;
    }

    // ==================== Helper Methods ====================

    /**
     * האם התשובה הצליחה?
     */
    public boolean isSuccessful() {
        return success != null && success && errorMessage == null;
    }

    /**
     * האם יש מקורות?
     */
    public boolean hasSources() {
        return sources != null && !sources.isEmpty();
    }

    /**
     * כמה מקורות?
     */
    public int getSourceCount() {
        return sources != null ? sources.size() : 0;
    }

    /**
     * האם הביטחון גבוה? (מעל 0.8)
     */
    public boolean isHighConfidence() {
        return confidence != null && confidence >= 0.8;
    }

    /**
     * האם הביטחון נמוך? (מתחת 0.5)
     */
    public boolean isLowConfidence() {
        return confidence != null && confidence < 0.5;
    }

    /**
     * רמת הביטחון כמחרוזת
     */
    public String getConfidenceLevel() {
        if (confidence == null) {
            return "לא ידוע";
        }

        if (confidence >= 0.8) {
            return "גבוה";
        } else if (confidence >= 0.5) {
            return "בינוני";
        } else {
            return "נמוך";
        }
    }

    /**
     * קבלת המקור העיקרי
     */
    public Source getPrimarySource() {
        if (sources == null || sources.isEmpty()) {
            return null;
        }

        return sources.stream()
                .max((s1, s2) -> {
                    if (s1.isPrimary && !s2.isPrimary) return 1;
                    if (!s1.isPrimary && s2.isPrimary) return -1;
                    
                    double score1 = s1.getRelevanceScore() != null ? s1.getRelevanceScore() : 0.0;
                    double score2 = s2.getRelevanceScore() != null ? s2.getRelevanceScore() : 0.0;
                    
                    return Double.compare(score1, score2);
                })
                .orElse(null);
    }

    /**
     * חישוב עלות משוערת (בדולרים)
     */
    public Double getEstimatedCost() {
        if (tokensUsed == null) {
            return 0.0;
        }
        return tokensUsed * 0.0001;
    }

    /**
     * האם התשובה ארוכה?
     */
    public boolean isLongAnswer() {
        return answer != null && answer.length() > 500;
    }

    /**
     * האם התשובה קצרה?
     */
    public boolean isShortAnswer() {
        return answer != null && answer.length() <= 50;
    }
}