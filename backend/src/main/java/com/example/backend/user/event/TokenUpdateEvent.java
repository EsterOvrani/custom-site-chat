package com.example.backend.user.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TokenUpdateEvent extends ApplicationEvent {
    
    private final Long userId;
    private final Long tokensUsed;
    private final Long tokensRemaining;
    private final Double usagePercentage;
    
    public TokenUpdateEvent(Object source, Long userId, Long tokensUsed, 
                           Long tokensRemaining, Double usagePercentage) {
        super(source);
        this.userId = userId;
        this.tokensUsed = tokensUsed;
        this.tokensRemaining = tokensRemaining;
        this.usagePercentage = usagePercentage;
    }
}