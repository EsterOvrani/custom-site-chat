package com.example.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionSummary {

    private String question;           // שאלה מנורמלת
    private Integer count;              // כמה פעמים נשאלה
    private List<String> examples;      // דוגמאות לניסוחים שונים
}