package com.example.backend.query.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicQueryRequest {
    
    @NotBlank(message = "Secret key is required")
    private String secretKey;
    
    @NotBlank(message = "Question is required")
    @Size(min = 1, max = 2000)
    private String question;
    
    // אופציונלי - לשמירה מקומית בצד לקוח
    private String sessionId;
}