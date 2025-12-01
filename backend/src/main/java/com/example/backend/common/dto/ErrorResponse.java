package com.example.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder; 
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder  
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    // if the action succsid
    private boolean success;
    
    // error code (to client side)
    private String errorCode;
    
    // error message
    private String message;
    
    // additional details on the error
    private String details;
    
    // time of the error ocore
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    // path of ocore error
    private String path;
    
    // error validation by fields
    private Map<String, String> fieldErrors;
    
    // create a simple response
    public static ErrorResponse of(String errorCode, String message) {
        return ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    // create error with details
    public static ErrorResponse of(String errorCode, String message, String details) {
        return ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    // create error resonse with error validation details 
    public static ErrorResponse withFieldErrors(
            String errorCode, 
            String message, 
            Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .fieldErrors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
