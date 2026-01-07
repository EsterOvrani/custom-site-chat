package com.example.backend.analytics.dto;

import lombok.Data;
import java.util.List;

@Data
public class SaveQuestionsRequest {
    private String secretKey;      
    private List<String> questions; 
    private String siteCategory;    
}