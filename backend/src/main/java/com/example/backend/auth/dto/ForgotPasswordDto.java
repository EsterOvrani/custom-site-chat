// backend/src/main/java/com/example/backend/auth/dto/ForgotPasswordDto.java
package com.example.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordDto {
    
    @NotBlank(message = "אימייל הוא שדה חובה")
    @Email(message = "כתובת אימייל לא תקינה")
    private String email;
}