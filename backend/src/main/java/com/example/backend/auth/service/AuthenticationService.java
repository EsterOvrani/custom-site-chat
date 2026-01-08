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

    @Autowired
    private TestConfig testConfig;

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

    // ==================== Authentication ====================

    // Authenticate user with email and password
    public User authenticate(LoginUserDto input) {
        User user = userRepository.findByUsername(input.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", input.getUsername()));

        if (!user.isEnabled()) {
            throw AuthenticationException.userNotVerified();
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getUsername(),
                        input.getPassword()
                )
        );

        return user;
    }

    // ==================== Registration ====================

    // Register new user and save to DB with enabled=false
    public User register(RegisterUserDto input) {
        log.info("Registration request for: {}", input.getEmail());

        // Check if email exists
        Optional<User> existingEmail = userRepository.findByEmail(input.getEmail());
        if (existingEmail.isPresent()) {
            User user = existingEmail.get();
            if (!user.isEnabled()) {
                // Not verified - delete and allow re-registration
                userRepository.delete(user);
                log.info("Deleted unverified user for re-registration: {}", input.getEmail());
            } else {
                throw new DuplicateResourceException("משתמש", "אימייל", input.getEmail());
            }
        }

        // Check if username exists
        Optional<User> existingUsername = userRepository.findByUsername(input.getUsername());
        if (existingUsername.isPresent()) {
            User user = existingUsername.get();
            if (!user.isEnabled()) {
                userRepository.delete(user);
                log.info("Deleted unverified user with same username: {}", input.getUsername());
            } else {
                throw new DuplicateResourceException("משתמש", "שם משתמש", input.getUsername());
            }
        }

        // Generate verification code
        String verificationCode = testConfig.isTestModeEnabled()
                ? testConfig.getFixedVerificationCode()
                : generateVerificationCode();

        // Create user in DB with enabled=false
        User user = new User();
        user.setEmail(input.getEmail());
        user.setUsername(input.getUsername());
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setFirstName(input.getFirstName());
        user.setLastName(input.getLastName());
        user.setEnabled(false);
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        User savedUser = userRepository.save(user);
        log.info("User created (not verified): {}", savedUser.getEmail());

        // Send verification email
        if (!testConfig.isBypassEmailVerification()) {
            sendVerificationEmail(input.getEmail(), verificationCode);
        } else {
            log.warn("TEST MODE: Email not sent, use code: {}", verificationCode);
        }

        return savedUser;
    }

    // Check if username exists (including unverified users)
    public boolean usernameExists(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.isPresent();
    }

    // Check if email exists (including unverified users)
    public boolean emailExists(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.isPresent();
    }

    // Check if email is verified
    public boolean isEmailVerified(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        return userOpt.get().isEnabled();
    }

    // ==================== Verification ====================
    
    // VERIFY IN REGISTER MODE:
    // Verify user email with code
    public User verifyUser(VerifyUserDto input) {
        log.info("Verifying user: {}", input.getEmail());

        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", input.getEmail()));

        if (user.isEnabled()) {
            throw new ValidationException("email", "החשבון כבר מאומת");
        }

        // Check verification code
        boolean isValidCode = false;

        if (testConfig.isTestModeEnabled() &&
                input.getVerificationCode().equals(testConfig.getFixedVerificationCode())) {
            isValidCode = true;
            log.warn("TEST MODE: Verification bypassed with fixed code");
        } else if (user.isVerificationCodeValid(input.getVerificationCode())) {
            isValidCode = true;
        }

        if (!isValidCode) {
            if (user.getVerificationCodeExpiresAt() != null &&
                    user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
                throw new ValidationException("verificationCode", "קוד האימות פג תוקף");
            }
            throw new ValidationException("verificationCode", "קוד אימות שגוי");
        }

        // Verify user
        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);

        User savedUser = userRepository.save(user);
        log.info("User verified: {}", savedUser.getEmail());

        return savedUser;
    }

    // Verify user by link
    public User verifyUserByLink(String email, String code) {
        VerifyUserDto dto = new VerifyUserDto();
        dto.setEmail(email);
        dto.setVerificationCode(code);
        return verifyUser(dto);
    }

    // VERIFY IN CHANGE PASSWORD MODE:
    // Send password reset code to email
    public void forgotPassword(String email) {
        log.info("Forgot password request for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", email));

        String resetCode = generateVerificationCode();

        user.setResetPasswordCode(resetCode);
        user.setResetPasswordCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        try {
            emailService.sendPasswordResetEmail(email, resetCode);
            log.info("Password reset email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email", e);
            throw new RuntimeException("נכשל בשליחת מייל איפוס סיסמה");
        }
    }

    // Validate password reset code
    public boolean verifyResetCode(String email, String resetCode) {
        log.info("Verifying reset code for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", email));

        boolean isValid = user.isResetPasswordCodeValid(resetCode);

        if (isValid) {
            log.info("Reset code verified for: {}", email);
        } else {
            log.warn("Invalid reset code for: {}", email);
        }

        return isValid;
    }

    // Resend verification code to email
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", email));

        if (user.isEnabled()) {
            throw new ValidationException("email", "החשבון כבר מאומת");
        }

        String newCode = testConfig.isTestModeEnabled()
                ? testConfig.getFixedVerificationCode()
                : generateVerificationCode();

        user.setVerificationCode(newCode);
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        if (!testConfig.isBypassEmailVerification()) {
            sendVerificationEmail(email, newCode);
        } else {
            log.warn("TEST MODE: Resent code: {}", newCode);
        }

        log.info("Verification code resent to: {}", email);
    }

    // ==================== Change password ====================

    // Set new password after code verification
    public void setNewPassword(String email, String newPassword) {
        log.info("Setting new password for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", email));

        if (user.getResetPasswordCode() == null) {
            throw new ValidationException("resetCode", "לא נמצא קוד איפוס תקף. יש לבקש קוד חדש");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.clearResetPasswordCode();
        user.clearTempPassword();

        userRepository.save(user);

        log.info("Password changed successfully for: {}", email);
    }

    // Legacy: verify code and set password in one step
    public void resetPassword(String email, String resetCode, String newPassword) {
        log.info("Reset password attempt for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("משתמש", email));

        if (!user.isResetPasswordCodeValid(resetCode)) {
            throw new ValidationException("resetCode", "קוד איפוס לא תקין או שפג תוקפו");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.clearResetPasswordCode();
        user.clearTempPassword();

        userRepository.save(user);

        log.info("Password reset successful for: {}", email);
    }

    // ==================== Private Helpers ====================

    private void sendVerificationEmail(String email, String verificationCode) {
        String subject = "Account Verification";

        try {
            emailService.sendVerificationEmail(email, subject, verificationCode);
            log.info("Verification email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send verification email", e);
            if (!testConfig.isTestModeEnabled()) {
                throw new RuntimeException("נכשל בשליחת מייל אימות");
            }
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}