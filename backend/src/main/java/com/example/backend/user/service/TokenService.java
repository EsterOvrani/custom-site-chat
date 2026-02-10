package com.example.backend.user.service;

import com.example.backend.user.model.User;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.common.exception.InsufficientTokensException;
import com.example.backend.user.event.TokenUpdateEvent;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * Check if user has enough tokens
     */
    public boolean hasTokensAvailable(User user, long requiredTokens) {
        return user.hasTokensAvailable(requiredTokens);
    }

    /**
     * Consume tokens for user - throws exception if insufficient
     */
    @Transactional
    public void consumeTokens(User user, long tokens) {
        if (!user.hasTokensAvailable(tokens)) {
            log.warn("⚠️ User {} tried to use {} tokens but only has {} remaining", 
                user.getId(), tokens, user.getRemainingTokens());
            throw new InsufficientTokensException(
                String.format("Insufficient tokens. Required: %d, Available: %d", 
                    tokens, user.getRemainingTokens())
            );
        }

        user.consumeTokens(tokens);
        userRepository.save(user);
        
        log.info("✅ Consumed {} tokens for user {}. Remaining: {}/{}", 
            tokens, user.getId(), user.getRemainingTokens(), user.getTokenQuota());
        
        // ✅ פרסם event על השינוי
        publishTokenUpdateEvent(user);
    }

// ✅ מתודה חדשה לפרסום event
private void publishTokenUpdateEvent(User user) {
    TokenUpdateEvent event = new TokenUpdateEvent(
        this,
        user.getId(),
        user.getTokensUsed(),
        user.getRemainingTokens(),
        user.getTokenUsagePercentage()
    );
    
    eventPublisher.publishEvent(event);
    log.debug("📢 Published token update event for user {}", user.getId());
}

    /**
     * Get token usage info for user
     */
    public TokenUsageInfo getTokenUsage(User user) {
        return TokenUsageInfo.builder()
            .quota(user.getTokenQuota())
            .used(user.getTokensUsed())
            .remaining(user.getRemainingTokens())
            .usagePercentage(user.getTokenUsagePercentage())
            .lastReset(user.getLastTokenReset())
            .build();
    }

    /**
     * Reset tokens for user (admin function)
     */
    @Transactional
    public void resetTokens(User user) {
        user.resetTokens();
        userRepository.save(user);
        log.info("🔄 Reset tokens for user {}", user.getId());
    }

    /**
     * Update token quota (admin function)
     */
    @Transactional
    public void updateTokenQuota(User user, long newQuota) {
        user.setTokenQuota(newQuota);
        userRepository.save(user);
        log.info("📊 Updated token quota for user {} to {}", user.getId(), newQuota);
    }

    @lombok.Data
    @lombok.Builder
    public static class TokenUsageInfo {
        private Long quota;
        private Long used;
        private Long remaining;
        private Double usagePercentage;
        private java.time.LocalDateTime lastReset;
    }
}