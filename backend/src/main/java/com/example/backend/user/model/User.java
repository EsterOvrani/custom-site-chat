// backend/src/main/java/com/example/backend/user/model/User.java
package com.example.backend.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "auth_provider")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Collection Fields ====================
    
    @Column(name = "collection_name", unique = true)
    private String collectionName;

    @Column(name = "collection_secret_key", unique = true)
    private String collectionSecretKey;

    @Column(name = "collection_created_at")
    private LocalDateTime collectionCreatedAt;

    @Column(name = "embed_code", columnDefinition = "TEXT")
    private String embedCode;

    // ==================== Verification Fields ====================
    
    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_code_expires_at")
    private LocalDateTime verificationCodeExpiresAt;

    // ==================== 🆕 Password Reset Fields ====================
    
    @Column(name = "reset_password_code")
    private String resetPasswordCode;

    @Column(name = "reset_password_code_expires_at")
    private LocalDateTime resetPasswordCodeExpiresAt;

    @Column(name = "temp_password")
    private String tempPassword; // סיסמה זמנית לפני hash (למשתמשי Google)

    // ==================== Lifecycle Callbacks ====================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== Helper Methods ====================

    public boolean hasCollection() {
        return collectionName != null && collectionSecretKey != null;
    }

    public String generateCollectionName() {
        return "user_" + this.id + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String generateSecretKey() {
        return "sk_" + UUID.randomUUID().toString().replace("-", "");
    }

    // ==================== UserDetails Implementation ====================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }


    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // ==================== Verification Methods ====================

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public void setVerificationCodeExpiresAt(LocalDateTime verificationCodeExpiresAt) {
        this.verificationCodeExpiresAt = verificationCodeExpiresAt;
    }

    public String getVerificationCode() {
        return this.verificationCode;
    }

    public LocalDateTime getVerificationCodeExpiresAt() {
        return this.verificationCodeExpiresAt;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    /**
     * בדיקה אם קוד האימות תקף
     */
    public boolean isVerificationCodeValid(String code) {
        if (this.verificationCode == null || this.verificationCodeExpiresAt == null) {
            return false;
        }
        
        if (LocalDateTime.now().isAfter(this.verificationCodeExpiresAt)) {
            return false;
        }
        
        return this.verificationCode.equals(code);
    }

    /**
     * ניקוי קוד אימות
     */
    public void clearVerificationCode() {
        this.verificationCode = null;
        this.verificationCodeExpiresAt = null;
    }

    // ==================== 🆕 Password Reset Methods ====================

    /**
     * בדיקה אם קוד איפוס הסיסמה תקף
     */
    public boolean isResetPasswordCodeValid(String code) {
        if (this.resetPasswordCode == null || this.resetPasswordCodeExpiresAt == null) {
            return false;
        }
        
        if (LocalDateTime.now().isAfter(this.resetPasswordCodeExpiresAt)) {
            return false;
        }
        
        return this.resetPasswordCode.equals(code);
    }

    /**
     * ניקוי קוד איפוס סיסמה
     */
    public void clearResetPasswordCode() {
        this.resetPasswordCode = null;
        this.resetPasswordCodeExpiresAt = null;
    }

    /**
     * ניקוי סיסמה זמנית (אחרי שינוי סיסמה)
     */
    public void clearTempPassword() {
        this.tempPassword = null;
    }

    // ==================== Enum ====================

    public enum AuthProvider {
        LOCAL,
        GOOGLE
    }
}