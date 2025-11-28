package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

import jakarta.annotation.PostConstruct;


@Configuration
@Getter
public class TestConfig {
    
    @Value("${app.test-mode.enabled:false}")
    private boolean testModeEnabled;
    
    @Value("${app.test-mode.bypass-email-verification:false}")
    private boolean bypassEmailVerification;
    
    @Value("${app.test-mode.fixed-verification-code:999999}")
    private String fixedVerificationCode;
    
    // 🔍 הוסף את זה
    @PostConstruct
    public void logConfig() {
        System.out.println("========================================");
        System.out.println("🔧 TEST CONFIG LOADED:");
        System.out.println("   testModeEnabled = " + testModeEnabled);
        System.out.println("   bypassEmailVerification = " + bypassEmailVerification);
        System.out.println("   fixedVerificationCode = " + fixedVerificationCode);
        System.out.println("========================================");
    }
}