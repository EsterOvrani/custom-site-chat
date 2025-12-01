package com.example.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDto {
    
    @NotBlank(message = "אימייל הוא שדה חובה")
    @Email(message = "כתובת אימייל לא תקינה")
    private String email;
    
    @NotBlank(message = "קוד איפוס הוא שדה חובה")
    @Size(min = 6, max = 6, message = "קוד איפוס חייב להכיל 6 ספרות")
    private String resetCode;
    
    @NotBlank(message = "סיסמה חדשה היא שדה חובה")
    @Size(min = 6, message = "סיסמה חייבת להכיל לפחות 6 תווים")
    private String newPassword;
}