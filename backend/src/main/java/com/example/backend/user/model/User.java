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

    // ==================== Basic Info ====================

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

    // ==================== Auth Provider ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "google_id")
    private String googleId;

    // ==================== Timestamps ====================

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

    @Builder.Default
    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_code_expires_at")
    private LocalDateTime verificationCodeExpiresAt;

    // ==================== Password Reset Fields ====================

    @Column(name = "reset_password_code")
    private String resetPasswordCode;

    @Column(name = "reset_password_code_expires_at")
    private LocalDateTime resetPasswordCodeExpiresAt;

    @Column(name = "temp_password")
    private String tempPassword;

    // ==================== Lifecycle ====================

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
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

    // ==================== UserDetails ====================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
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

    // ==================== Verification ====================

    public boolean isVerificationCodeValid(String code) {
        if (verificationCode == null || verificationCodeExpiresAt == null) 
            return false;
        if (LocalDateTime.now().isAfter(verificationCodeExpiresAt)) 
            return false;
        return verificationCode.equals(code);
    }

    public void clearVerificationCode() {
        this.verificationCode = null;
        this.verificationCodeExpiresAt = null;
    }

    // ==================== Password Reset ====================

    public boolean isResetPasswordCodeValid(String code) {
        if (resetPasswordCode == null || resetPasswordCodeExpiresAt == null) 
            return false;
        if (LocalDateTime.now().isAfter(resetPasswordCodeExpiresAt)) 
            return false;
        return resetPasswordCode.equals(code);
    }

    public void clearTempPassword() {
        this.tempPassword = null;
    }

    // ==================== Enum ====================

    public enum AuthProvider {
        LOCAL,
        GOOGLE
    }
}
