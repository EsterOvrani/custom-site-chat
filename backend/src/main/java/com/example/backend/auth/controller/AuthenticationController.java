// backend/src/main/java/com/example/backend/auth/controller/AuthenticationController.java
package com.example.backend.auth.controller;

import com.example.backend.auth.dto.GoogleAuthRequest;
import com.example.backend.auth.dto.LoginUserDto;
import com.example.backend.auth.dto.RegisterUserDto;
import com.example.backend.auth.dto.VerifyUserDto;
import com.example.backend.auth.dto.ForgotPasswordDto;
import com.example.backend.auth.dto.ResetPasswordDto;
import com.example.backend.auth.dto.VerifyResetCodeDto;
import com.example.backend.auth.dto.SetNewPasswordDto;
import com.example.backend.auth.service.AuthenticationService;
import com.example.backend.auth.service.GoogleOAuthService;
import com.example.backend.auth.service.JwtService;
import com.example.backend.collection.dto.CollectionInfoResponse;
import com.example.backend.collection.service.CollectionService;
import com.example.backend.user.model.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {
    
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;
    private final GoogleOAuthService googleOAuthService;
    private final CollectionService collectionService;  

    // ==================== EXISTING: Google Login ====================
    
    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody GoogleAuthRequest request) {
        try {
            log.info("Google login attempt");
            
            User user = googleOAuthService.authenticateGoogleUser(request.getCredential());
            
            String jwtToken = jwtService.generateToken(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", jwtToken);
            response.put("expiresIn", jwtService.getExpirationTime());
            response.put("user", Map.of(
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFirstName() + " " + user.getLastName(),
                "profilePictureUrl", user.getProfilePictureUrl() != null ? user.getProfilePictureUrl() : ""
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Google login failed", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Google authentication failed");
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // ==================== REGISTER (Step 1 - Creates Pending Registration) ====================
    
    @PostMapping({"/signup", "/register"})
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterUserDto registerUserDto) {
        log.info("========================================");
        log.info("🌐 REGISTER REQUEST RECEIVED");
        log.info("   Email: {}", registerUserDto.getEmail());
        log.info("   Username: {}", registerUserDto.getUsername());
        log.info("========================================");
        
        // 🆕 יוצר רישום ממתין (לא שומר ב-DB עדיין!)
        String verificationCode = authenticationService.createPendingRegistration(registerUserDto);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "קוד אימות נשלח למייל. המשתמש ייווצר רק לאחר אימות הקוד.");
        response.put("email", registerUserDto.getEmail());
        
        log.info("========================================");
        log.info("📤 PENDING REGISTRATION CREATED");
        log.info("   Email: {}", registerUserDto.getEmail());
        log.info("   Verification code sent (not saved to DB yet)");
        log.info("========================================");
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== VERIFY (Step 2 - Creates User in DB) ====================

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {
        log.info("🔐 Verify registration for: {}", verifyUserDto.getEmail());
        
        // 🆕 רק כאן המשתמש באמת נוצר ב-DB!
        User createdUser = authenticationService.verifyAndCreateUser(verifyUserDto);
        
        // 🆕 יצירת טוקן כדי שהמשתמש יוכל להיכנס ישירות לדשבורד
        String jwtToken = jwtService.generateToken(createdUser);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "החשבון אומת ונוצר בהצלחה!");
        response.put("token", jwtToken);
        response.put("expiresIn", jwtService.getExpirationTime());
        response.put("user", Map.of(
            "id", createdUser.getId(),
            "username", createdUser.getUsername(),
            "email", createdUser.getEmail(),
            "fullName", createdUser.getFirstName() + " " + createdUser.getLastName()
        ));
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyUserByLink(
            @RequestParam String email, 
            @RequestParam String code) {
        
        VerifyUserDto verifyUserDto = new VerifyUserDto();
        verifyUserDto.setEmail(email);
        verifyUserDto.setVerificationCode(code);
        
        authenticationService.verifyAndCreateUser(verifyUserDto);
        
        return ResponseEntity.status(302)
                .header("Location", "/login?verified=true")
                .build();
    }

    // ==================== LOGIN ====================
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> authenticate(
            @RequestBody LoginUserDto loginUserDto) {
        
        User authenticatedUser = authenticationService.authenticate(loginUserDto);
        
        String jwtToken = jwtService.generateToken(authenticatedUser);
        
        CollectionInfoResponse collectionInfo = null;
        try {
            collectionInfo = collectionService.getOrCreateUserCollection(authenticatedUser);
        } catch (Exception e) {
            log.warn("Could not create collection during login", e);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", jwtToken);
        response.put("expiresIn", jwtService.getExpirationTime());
        response.put("user", Map.of(
            "username", authenticatedUser.getUsername(),
            "email", authenticatedUser.getEmail(),
            "fullName", authenticatedUser.getFirstName() + " " + authenticatedUser.getLastName()
        ));
        
        if (collectionInfo != null) {
            response.put("collection", Map.of(
                "hasCollection", true,
                "collectionName", collectionInfo.getCollectionName()
            ));
        }
        
        return ResponseEntity.ok(response);
    }

    // ==================== RESEND VERIFICATION ====================

    @PostMapping("/resend")
    public ResponseEntity<Map<String, Object>> resendVerificationCode(@RequestParam String email) {
        authenticationService.resendVerificationCode(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "קוד אימות נשלח מחדש");
        
        return ResponseEntity.ok(response);
    }

    // ==================== FORGOT PASSWORD (Step 1) ====================

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordDto request) {
        
        log.info("🔐 Forgot password request for: {}", request.getEmail());
        
        authenticationService.forgotPassword(request.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "קוד איפוס סיסמה נשלח למייל שלך");
        
        return ResponseEntity.ok(response);
    }

    // ==================== VERIFY RESET CODE (Step 2) ====================

    @PostMapping("/verify-reset-code")
    public ResponseEntity<Map<String, Object>> verifyResetCode(
            @Valid @RequestBody VerifyResetCodeDto request) {
        
        log.info("🔐 Verify reset code request for: {}", request.getEmail());
        
        boolean isValid = authenticationService.verifyResetCode(
            request.getEmail(), 
            request.getResetCode()
        );
        
        Map<String, Object> response = new HashMap<>();
        
        if (isValid) {
            response.put("success", true);
            response.put("message", "קוד האיפוס אומת בהצלחה");
            response.put("verified", true);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("error", "קוד איפוס לא תקין או שפג תוקפו");
            response.put("verified", false);
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== SET NEW PASSWORD (Step 3) ====================

    @PostMapping("/set-new-password")
    public ResponseEntity<Map<String, Object>> setNewPassword(
            @Valid @RequestBody SetNewPasswordDto request) {
        
        log.info("🔐 Set new password request for: {}", request.getEmail());
        
        authenticationService.setNewPassword(
            request.getEmail(), 
            request.getNewPassword()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "הסיסמה שונתה בהצלחה! כעת תוכל להתחבר עם הסיסמה החדשה");
        
        return ResponseEntity.ok(response);
    }

    // ==================== RESET PASSWORD (Legacy - combines Step 2+3) ====================

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @Valid @RequestBody ResetPasswordDto request) {
        
        log.info("🔐 Reset password request for: {}", request.getEmail());
        
        authenticationService.resetPassword(
            request.getEmail(), 
            request.getResetCode(), 
            request.getNewPassword()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "הסיסמה שונתה בהצלחה! כעת תוכל להתחבר עם הסיסמה החדשה");
        
        return ResponseEntity.ok(response);
    }

    // ==================== CHECK AVAILABILITY ====================

    @GetMapping("/check-username/{username}")
    public ResponseEntity<Map<String, Object>> checkUsername(@PathVariable String username) {
        boolean exists = authenticationService.usernameExists(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("available", !exists);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-email/{email}")
    public ResponseEntity<Map<String, Object>> checkEmail(@PathVariable String email) {
        boolean exists = authenticationService.emailExists(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("available", !exists);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-verified/{email}")
    public ResponseEntity<Map<String, Object>> checkIfVerified(@PathVariable String email) {
        boolean verified = authenticationService.isEmailVerified(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("verified", verified);
        response.put("email", email);
        
        return ResponseEntity.ok(response);
    }

    // ==================== CHECK PENDING REGISTRATION ====================

    @GetMapping("/check-pending/{email}")
    public ResponseEntity<Map<String, Object>> checkPendingRegistration(@PathVariable String email) {
        boolean hasPending = authenticationService.hasPendingRegistration(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasPending", hasPending);
        response.put("email", email);
        
        return ResponseEntity.ok(response);
    }

    // ==================== STATUS ====================

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkStatus() {
        Map<String, Object> response = new HashMap<>();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && 
            authentication.isAuthenticated() && 
            authentication.getPrincipal() instanceof User) {
            
            User user = (User) authentication.getPrincipal();
            
            response.put("success", true);
            response.put("authenticated", true);
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("fullName", user.getFirstName() + " " + user.getLastName());
            userInfo.put("profilePictureUrl", user.getProfilePictureUrl() != null ? user.getProfilePictureUrl() : "");
            response.put("user", userInfo);
        } else {
            response.put("success", true);
            response.put("authenticated", false);
        }
        
        return ResponseEntity.ok(response);
    }

    // ==================== LOGOUT ====================

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        SecurityContextHolder.clearContext();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out successfully");
        
        return ResponseEntity.ok(response);
    }
}