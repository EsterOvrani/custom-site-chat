// backend/src/main/java/com/example/backend/auth/service/AuthenticationService.java
package com.example.backend.auth.service;

import com.example.backend.auth.dto.LoginUserDto;
import com.example.backend.auth.dto.RegisterUserDto;
import com.example.backend.auth.dto.VerifyUserDto;
import com.example.backend.user.model.User;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.common.infrastructure.email.EmailService;
import com.example.backend.config.TestConfig;
import com.example.backend.common.exception.AuthenticationException;
import com.example.backend.common.exception.ResourceNotFoundException;
import com.example.backend.common.exception.DuplicateResourceException;
import com.example.backend.common.exception.ValidationException; 

import jakarta.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }
    
    @Autowired
    private TestConfig testConfig; 
    
    // ==================== EXISTING: Signup ====================
    
    public User signup(RegisterUserDto input) {
        // 🔍 הוסף את זה בשורה הראשונה של הפונקציה
        log.info("========================================");
        log.info("🔵 SIGNUP STARTED");
        log.info("   Email: {}", input.getEmail());
        log.info("   Username: {}", input.getUsername());
        log.info("========================================");
        
        User user = new User();
        user.setUsername(input.getUsername());
        user.setEmail(input.getEmail());
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setFirstName(input.getFirstName());
        user.setLastName(input.getLastName());

        // 🔍 הוסף את זה לפני ה-if
        log.info("========================================");
        log.info("🔍 TEST MODE CHECK:");
        log.info("   testConfig = {}", testConfig);
        log.info("   isBypassEmailVerification() = {}", testConfig.isBypassEmailVerification());
        log.info("   isTestModeEnabled() = {}", testConfig.isTestModeEnabled());
        log.info("========================================");

        // ⭐ Test Mode Logic
        if (testConfig.isBypassEmailVerification()) {
            log.warn("🔶 TEST MODE ACTIVE - BYPASSING EMAIL VERIFICATION!");
            user.setEnabled(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            log.warn("🔶 TEST MODE: User automatically verified!");
        } else {
            log.info("✅ NORMAL MODE - Email verification required");
            user.setVerificationCode(
                testConfig.isTestModeEnabled() 
                    ? testConfig.getFixedVerificationCode()
                    : generateVerificationCode()
            );
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
            user.setEnabled(false);
            
            log.info("📧 Verification code generated: {}", user.getVerificationCode());
            log.info("📧 Sending verification email to: {}", user.getEmail());
            
            sendVerificationEmail(user);
        }

        User savedUser = userRepository.save(user);
        
        // 🔍 הוסף את זה בסוף
        log.info("========================================");
        log.info("✅ SIGNUP COMPLETED");
        log.info("   User ID: {}", savedUser.getId());
        log.info("   Email: {}", savedUser.getEmail());
        log.info("   Enabled: {}", savedUser.isEnabled());
        log.info("   Has verification code: {}", savedUser.getVerificationCode() != null);
        log.info("========================================");

        return savedUser;
    }

    // ==================== AUTHENTICATE ====================
    
    public User authenticate(LoginUserDto input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", input.getEmail()));

        if (!user.isEnabled()) {
            throw AuthenticationException.userNotVerified();
        }
        
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        return user;
    }

    // ==================== VERIFY USER ====================
    
    public void verifyUser(VerifyUserDto input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", input.getEmail()));
        
        // ⭐ Test Mode: קוד קבוע תמיד נכון
        if (testConfig.isTestModeEnabled() && 
            input.getVerificationCode().equals(testConfig.getFixedVerificationCode())) {
            user.setEnabled(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            userRepository.save(user);
            log.warn("🔶 TEST MODE: Verification bypassed with fixed code!");
            return;
        }
        
        // Regular verification logic
        if (user.getVerificationCodeExpiresAt() == null ||
            user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("verificationCode", "קוד האימות פג תוקף");
        }
        
        if (!user.getVerificationCode().equals(input.getVerificationCode())) {
            throw new ValidationException("verificationCode", "קוד אימות שגוי");
        }
        
        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
        
        log.info("✅ User verified: {}", input.getEmail());
    }

    // ==================== RESEND VERIFICATION CODE ====================
    
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", email));
        
        if (user.isEnabled()) {
            throw new ValidationException("email", "החשבון כבר מאומת");
        }
        
        // Use fixed code in test mode, random otherwise
        String newCode = testConfig.isTestModeEnabled() 
            ? testConfig.getFixedVerificationCode() 
            : generateVerificationCode();
            
        user.setVerificationCode(newCode);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        
        userRepository.save(user);
        sendVerificationEmail(user);
        
        if (testConfig.isTestModeEnabled()) {
            log.warn("🔶 TEST MODE: Resent fixed code: {}", newCode);
        } else {
            log.info("🔄 Verification code resent to: {}", email);
        }
    }

    // ==================== IS EMAIL VERIFIED ====================
    
    public boolean isEmailVerified(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", email));
        return user.isEnabled();
    }

    // ==================== FORGOT PASSWORD ====================
    
    public void forgotPassword(String email) {
        log.info("🔐 Forgot password request for: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", email));
        
        // יצירת קוד איפוס (6 ספרות)
        String resetCode = generateVerificationCode();
        
        user.setResetPasswordCode(resetCode);
        user.setResetPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        
        userRepository.save(user);
        
        // שליחת מייל
        try {
            emailService.sendPasswordResetEmail(email, resetCode);
            log.info("✅ Password reset email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("❌ Failed to send password reset email", e);
            throw new RuntimeException("נכשל בשליחת מייל איפוס סיסמה");
        }
    }

    // ==================== RESET PASSWORD ====================
    
    public void resetPassword(String email, String resetCode, String newPassword) {
        log.info("🔐 Reset password attempt for: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", email));
        
        // בדיקת קוד
        if (!user.isResetPasswordCodeValid(resetCode)) {
            throw new ValidationException("resetCode", "קוד איפוס לא תקין או שפג תוקפו");
        }
        
        // עדכון סיסמה
        user.setPassword(passwordEncoder.encode(newPassword));
        
        // ניקוי קודים
        user.clearResetPasswordCode();
        user.clearTempPassword();
        
        userRepository.save(user);
        
        log.info("✅ Password reset successful for: {}", email);
    }

    // ==================== USERNAME/EMAIL EXISTS ====================
    
    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    // ==================== PRIVATE HELPERS ====================
    
    private void sendVerificationEmail(User user) {
        String subject = "Account Verification";
        String verificationCode = user.getVerificationCode();
        
        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, verificationCode);
        } catch (MessagingException e) {
            log.error("❌ Failed to send verification email", e);
            // בטסט מוד לא זורקים exception
            if (!testConfig.isTestModeEnabled()) {
                throw new RuntimeException("נכשל בשליחת מייל אימות");
            }
        }
    }
    
    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000; // 100000-999999
        return String.valueOf(code);
    }
}