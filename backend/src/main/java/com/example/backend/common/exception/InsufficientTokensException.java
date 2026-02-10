package com.example.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user doesn't have enough tokens
 */
public class InsufficientTokensException extends BaseException {
    
    public InsufficientTokensException(String message) {
        super(message, HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_TOKENS");
    }
    
    public InsufficientTokensException(String message, Throwable cause) {
        super(message, cause, HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_TOKENS");
    }
}