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
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    // 🆕 In-Memory storage for pending registrations (not saved to DB until verified)
    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

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

    // ==================== 🆕 PENDING REGISTRATION CLASS ====================
    
    private static class PendingRegistration {
        String email;
        String username;
        String password; // Already encoded
        String firstName;
        String lastName;
        String verificationCode;
        LocalDateTime expiresAt;
        LocalDateTime createdAt;

        PendingRegistration(RegisterUserDto dto, String encodedPassword, String verificationCode) {
            this.email = dto.getEmail();
            this.username = dto.getUsername();
            this.password = encodedPassword;
            this.firstName = dto.getFirstName();
            this.lastName = dto.getLastName();
            this.verificationCode = verificationCode;
            this.expiresAt = LocalDateTime.now().plusMinutes(15);
            this.createdAt = LocalDateTime.now();
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        boolean isCodeValid(String code) {
            return !isExpired() && verificationCode.equals(code);
        }
    }
    
    // ==================== CREATE PENDING REGISTRATION (Step 1) ====================
    
    public String createPendingRegistration(RegisterUserDto input) {
        log.info("========================================");
        log.info("🔵 CREATING PENDING REGISTRATION");
        log.info("   Email: {}", input.getEmail());
        log.info("   Username: {}", input.getUsername());
        log.info("========================================");

        // בדיקה אם המשתמש כבר קיים ב-DB
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new DuplicateResourceException("משתמש", "אימייל", input.getEmail());
        }
        
        if (userRepository.findByUsername(input.getUsername()).isPresent()) {
            throw new DuplicateResourceException("משתמש", "שם משתמש", input.getUsername());
        }

        // יצירת קוד אימות
        String verificationCode = testConfig.isTestModeEnabled() 
            ? testConfig.getFixedVerificationCode()
            : generateVerificationCode();

        // קידוד הסיסמה
        String encodedPassword = passwordEncoder.encode(input.getPassword());

        // שמירה ב-Memory (לא ב-DB!)
        PendingRegistration pending = new PendingRegistration(input, encodedPassword, verificationCode);
        pendingRegistrations.put(input.getEmail().toLowerCase(), pending);

        log.info("📧 Pending registration created");
        log.info("   Verification code: {}", verificationCode);
        log.info("   Expires at: {}", pending.expiresAt);

        // ⭐ Test Mode: אם bypass מופעל, לא שולחים מייל אבל עדיין צריך אימות
        if (testConfig.isBypassEmailVerification()) {
            log.warn("🔶 TEST MODE ACTIVE - Email not sent, but verification still required");
            log.warn("🔶 TEST MODE: Use code {} to verify", verificationCode);
        } else {
            // שליחת מייל
            sendVerificationEmail(input.getEmail(), verificationCode);
        }

        log.info("========================================");
        log.info("✅ PENDING REGISTRATION CREATED (NOT IN DB YET)");
        log.info("   Email: {}", input.getEmail());
        log.info("========================================");

        return verificationCode;
    }

    // ==================== VERIFY AND CREATE USER (Step 2) ====================
    
    public User verifyAndCreateUser(VerifyUserDto input) {
        String email = input.getEmail().toLowerCase();
        
        log.info("🔐 Verifying and creating user: {}", email);

        // בדיקה אם יש רישום ממתין
        PendingRegistration pending = pendingRegistrations.get(email);
        
        if (pending == null) {
            // אולי המשתמש כבר נוצר? (backward compatibility)
            Optional<User> existingUser = userRepository.findByEmail(input.getEmail());
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                if (user.isEnabled()) {
                    throw new ValidationException("email", "החשבון כבר אומת");
                }
                // Legacy flow - user exists but not verified
                return verifyExistingUser(user, input.getVerificationCode());
            }
            throw new ValidationException("email", "לא נמצא רישום ממתין. יש להירשם מחדש");
        }

        // בדיקת תוקף הקוד
        if (pending.isExpired()) {
            pendingRegistrations.remove(email);
            throw new ValidationException("verificationCode", "קוד האימות פג תוקף. יש להירשם מחדש");
        }

        // ⭐ Test Mode: קוד קבוע תמיד נכון
        boolean isValidCode = false;
        if (testConfig.isTestModeEnabled() && 
            input.getVerificationCode().equals(testConfig.getFixedVerificationCode())) {
            isValidCode = true;
            log.warn("🔶 TEST MODE: Verification bypassed with fixed code!");
        } else if (pending.isCodeValid(input.getVerificationCode())) {
            isValidCode = true;
        }

        if (!isValidCode) {
            throw new ValidationException("verificationCode", "קוד אימות שגוי");
        }

        // ✅ הקוד נכון - יוצרים את המשתמש ב-DB!
        User user = new User();
        user.setEmail(pending.email);
        user.setUsername(pending.username);
        user.setPassword(pending.password); // Already encoded
        user.setFirstName(pending.firstName);
        user.setLastName(pending.lastName);
        user.setEnabled(true); // מאומת מיד!
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);

        User savedUser = userRepository.save(user);

        // מחיקה מה-Memory
        pendingRegistrations.remove(email);

        log.info("========================================");
        log.info("✅ USER CREATED AND VERIFIED!");
        log.info("   User ID: {}", savedUser.getId());
        log.info("   Email: {}", savedUser.getEmail());
        log.info("   Username: {}", savedUser.getUsername());
        log.info("========================================");

        return savedUser;
    }

    // ==================== LEGACY: VERIFY EXISTING USER ====================
    
    private User verifyExistingUser(User user, String verificationCode) {
        // ⭐ Test Mode: קוד קבוע תמיד נכון
        if (testConfig.isTestModeEnabled() && 
            verificationCode.equals(testConfig.getFixedVerificationCode())) {
            user.setEnabled(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            userRepository.save(user);
            log.warn("🔶 TEST MODE: Legacy verification bypassed with fixed code!");
            return user;
        }
        
        // Regular verification logic
        if (user.getVerificationCodeExpiresAt() == null ||
            user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("verificationCode", "קוד האימות פג תוקף");
        }
        
        if (!user.getVerificationCode().equals(verificationCode)) {
            throw new ValidationException("verificationCode", "קוד אימות שגוי");
        }
        
        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
        
        log.info("✅ Legacy user verified: {}", user.getEmail());
        return user;
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

    // ==================== RESEND VERIFICATION CODE ====================
    
    public void resendVerificationCode(String email) {
        String emailLower = email.toLowerCase();
        
        // בדיקה אם יש רישום ממתין
        PendingRegistration pending = pendingRegistrations.get(emailLower);
        
        if (pending != null) {
            // יש רישום ממתין - מחדשים את הקוד
            String newCode = testConfig.isTestModeEnabled() 
                ? testConfig.getFixedVerificationCode() 
                : generateVerificationCode();
                
            pending.verificationCode = newCode;
            pending.expiresAt = LocalDateTime.now().plusMinutes(15);
            
            if (!testConfig.isBypassEmailVerification()) {
                sendVerificationEmail(email, newCode);
            }
            
            log.info("🔄 Verification code resent for pending registration: {}", email);
            return;
        }
        
        // Legacy: בדיקה אם המשתמש קיים ב-DB
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
        
        if (!testConfig.isBypassEmailVerification()) {
            sendVerificationEmail(user.getEmail(), newCode);
        }
        
        if (testConfig.isTestModeEnabled()) {
            log.warn("🔶 TEST MODE: Resent fixed code: {}", newCode);
        } else {
            log.info("🔄 Verification code resent to: {}", email);
        }
    }

    // ==================== CHECK PENDING REGISTRATION ====================
    
    public boolean hasPendingRegistration(String email) {
        PendingRegistration pending = pendingRegistrations.get(email.toLowerCase());
        if (pending == null) {
            return false;
        }
        if (pending.isExpired()) {
            pendingRegistrations.remove(email.toLowerCase());
            return false;
        }
        return true;
    }

    // ==================== IS EMAIL VERIFIED ====================
    
    public boolean isEmailVerified(String email) {
        // אם יש רישום ממתין - לא מאומת
        if (hasPendingRegistration(email)) {
            return false;
        }
        
        // בדיקה ב-DB
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        return userOpt.get().isEnabled();
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

    // ==================== VERIFY RESET CODE (Step 2) ====================

    public boolean verifyResetCode(String email, String resetCode) {
        log.info("🔐 Verifying reset code for: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", email));
        
        boolean isValid = user.isResetPasswordCodeValid(resetCode);
        
        if (isValid) {
            log.info("✅ Reset code verified for: {}", email);
        } else {
            log.warn("❌ Invalid reset code for: {}", email);
        }
        
        return isValid;
    }

    // ==================== SET NEW PASSWORD (Step 3) ====================

    public void setNewPassword(String email, String newPassword) {
        log.info("🔐 Setting new password for: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", email));
        
        // בדיקה שיש קוד איפוס תקף (המשתמש עבר את שלב 2)
        if (user.getResetPasswordCode() == null) {
            throw new ValidationException("resetCode", "לא נמצא קוד איפוס תקף. יש לבקש קוד חדש");
        }
        
        // עדכון סיסמה
        user.setPassword(passwordEncoder.encode(newPassword));
        
        // ניקוי קודים
        user.clearResetPasswordCode();
        user.clearTempPassword();
        
        userRepository.save(user);
        
        log.info("✅ Password changed successfully for: {}", email);
    }

    // ==================== RESET PASSWORD (Legacy - combines Step 2+3) ====================
    
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
        // בודק גם ברישומים ממתינים
        boolean inPending = pendingRegistrations.values().stream()
            .anyMatch(p -> !p.isExpired() && p.username.equalsIgnoreCase(username));
        
        return inPending || userRepository.findByUsername(username).isPresent();
    }

    public boolean emailExists(String email) {
        // בודק גם ברישומים ממתינים
        String emailLower = email.toLowerCase();
        PendingRegistration pending = pendingRegistrations.get(emailLower);
        boolean inPending = pending != null && !pending.isExpired();
        
        return inPending || userRepository.findByEmail(email).isPresent();
    }

    // ==================== PRIVATE HELPERS ====================
    
    private void sendVerificationEmail(String email, String verificationCode) {
        String subject = "Account Verification";
        
        try {
            emailService.sendVerificationEmail(email, subject, verificationCode);
            log.info("📧 Verification email sent to: {}", email);
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