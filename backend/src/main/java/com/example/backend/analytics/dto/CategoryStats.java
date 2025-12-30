package com.example.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryStats {

    private String category;        // שם הקטגוריה
    private Integer count;          // כמה פעמים הופיעה
    private Double percentage;      // אחוז מתוך כל השיחות
}