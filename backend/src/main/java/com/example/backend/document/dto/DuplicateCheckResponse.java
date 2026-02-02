package com.example.backend.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for duplicate file check
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateCheckResponse {
    
    /**
     * Whether a file with the same name exists
     */
    private boolean exists;
    
    /**
     * ID of the existing document (if exists)
     */
    private Long existingDocumentId;
    
    /**
     * Original file name
     */
    private String fileName;
    
    /**
     * Suggested new name (for add as new option)
     */
    private String suggestedName;
}
