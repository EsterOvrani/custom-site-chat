package com.example.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when trying to create a resource that already exists (duplicate email, duplicate username, etc.)
 */
public class DuplicateResourceException extends BaseException {
    
    private static final String ERROR_CODE = "DUPLICATE_RESOURCE";
    
    public DuplicateResourceException(String resourceName, String fieldName, String value) {
        super(
            String.format("%s עם %s '%s' כבר קיים במערכת", resourceName, fieldName, value),
            HttpStatus.CONFLICT,
            ERROR_CODE
        );
    }
    
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, ERROR_CODE);
    }
    
    public static DuplicateResourceException email(String email) {
        return new DuplicateResourceException("משתמש", "דואר אלקטרוני", email);
    }
    
    public static DuplicateResourceException username(String username) {
        return new DuplicateResourceException("משתמש", "שם משתמש", username);
    }
}