package com.example.backend.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryResponse {
    
    // התשובה עצמה
    private String answer;
    
    // מקורות (מסמכים רלוונטיים)
    private List<Source> sources;
    
    // מטריקות
    private Long responseTimeMs;
    private Double confidence;
    private Integer tokensUsed;
    
    // Inner class למקור
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Source {
        private String documentName;
        private String excerpt;
        private Double relevanceScore;
        private Boolean isPrimary;
    }
}