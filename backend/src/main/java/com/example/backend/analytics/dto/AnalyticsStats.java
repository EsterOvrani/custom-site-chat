package com.example.backend.analytics.dto;

import lombok.Data;

/**
 * Analytics Statistics DTO
 * 
 * Summary statistics about analytics data
 * Note: We no longer track "total sessions" - only question/category counts
 */
@Data
public class AnalyticsStats {
    
    // Questions stats
    private int totalQuestions;        // Total raw questions collected
    private int uniqueQuestions;       // Unique questions after consolidation
    private boolean questionsProcessed; // Whether questions have been summarized
    
    // Categories stats
    private int totalCategories;       // Total raw categories collected
    private int uniqueCategories;      // Unique categories after consolidation
    private boolean categoriesProcessed; // Whether categories have been summarized
}