package com.example.backend.document.dto;

import com.example.backend.document.model.Document.ProcessingStatus;
import com.example.backend.document.model.Document.ProcessingStage;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {

    private Long id;
    private Long userId;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String fileSizeFormatted;
    private String filePath;
    
    private ProcessingStatus processingStatus;
    private Integer processingProgress;
    
    // ⭐ חדש - שלב עיבוד נוכחי
    private ProcessingStage processingStage;
    private String processingStageDescription;
    
    private Integer characterCount;
    private Integer chunkCount;

    private Integer tokenCount;
    private String tokenCountFormatted;

    private Boolean active;
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;

    private ProcessingStatistics statistics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingStatistics {
        private Long processingTimeMs;
        private String processingTimeFormatted;
        private Integer embeddingsCount;
        private Double estimatedCost;
    }

    // ==================== Helper Methods ====================

    public boolean isProcessed() {
        return processingStatus == ProcessingStatus.COMPLETED;
    }

    public boolean isProcessing() {
        return processingStatus == ProcessingStatus.PROCESSING;
    }

    public boolean hasFailed() {
        return processingStatus == ProcessingStatus.FAILED;
    }

    public boolean isPending() {
        return processingStatus == ProcessingStatus.PENDING;
    }

    public String getFormattedFileSize() {
        if (fileSize == null) {
            return "לא ידוע";
        }

        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * ⭐ חדש - תיאור שלב עיבוד בעברית
     */
    public String getProcessingStageDescription() {
        if (processingStage == null) {
            return "";
        }
        
        return switch (processingStage) {
            case UPLOADING -> "מעלה לשרת...";
            case EXTRACTING_TEXT -> "מחלץ טקסט מהמסמך...";
            case CREATING_CHUNKS -> "מחלק לחלקים...";
            case CREATING_EMBEDDINGS -> "יוצר embeddings...";
            case STORING -> "שומר במאגר...";
            case COMPLETED -> "הושלם בהצלחה";
            case FAILED -> "נכשל";
        };
    }

    public String getStatusDescription() {
        return switch (processingStatus) {
            case PENDING -> "ממתין לעיבוד";
            case PROCESSING -> String.format("%s (%d%%)", 
                getProcessingStageDescription(), 
                processingProgress);
            case COMPLETED -> "הושלם בהצלחה";
            case FAILED -> "נכשל: " + (errorMessage != null ? errorMessage : "שגיאה לא ידועה");
        };
    }

    /**
     * פורמט ספירת טוקנים עם פסיקים
     */
    public String getFormattedTokenCount() {
        if (tokenCount == null) {
            return "לא זמין";
        }
        return String.format("%,d", tokenCount);
    }
}