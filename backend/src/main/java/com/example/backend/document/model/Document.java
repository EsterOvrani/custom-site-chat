package com.example.backend.document.model;

import com.example.backend.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "processing_status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "processing_progress")
    @Builder.Default
    private Integer processingProgress = 0;

    // ⭐ חדש - שלב עיבוד נוכחי
    @Column(name = "processing_stage")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProcessingStage processingStage = ProcessingStage.UPLOADING;

    @Column(name = "character_count")
    private Integer characterCount;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // ==================== Lifecycle ====================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.displayOrder == null) {
            this.displayOrder = 0;
        }
        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    // ==================== Processing Methods ====================

    /**
     * התחלת עיבוד מסמך
     */
    public void startProcessing() {
        this.processingStatus = ProcessingStatus.PROCESSING;
        this.processingProgress = 0;
        this.processingStage = ProcessingStage.UPLOADING;
        this.errorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * עדכון שלב עיבוד
     */
    public void updateStage(ProcessingStage stage, int progress) {
        this.processingStage = stage;
        this.processingProgress = Math.min(100, Math.max(0, progress));
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * סיום עיבוד בהצלחה
     */
    public void markAsCompleted(int characterCount, int chunkCount) {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.processingStage = ProcessingStage.COMPLETED;
        this.processingProgress = 100;
        this.characterCount = characterCount;
        this.chunkCount = chunkCount;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.errorMessage = null;
    }
    
    /**
     * סיום עיבוד עם ספירת טוקנים
     */
    public void markAsCompletedWithTokens(int characterCount, int chunkCount, int tokenCount) {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.processingStage = ProcessingStage.COMPLETED;
        this.processingProgress = 100;
        this.characterCount = characterCount;
        this.chunkCount = chunkCount;
        this.tokenCount = tokenCount;  // ⭐ החדש!
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    /**
     * סימון כנכשל
     */
    public void markAsFailed(String errorMessage) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.processingStage = ProcessingStage.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * עדכון אחוז ההתקדמות
     */
    public void updateProgress(int progress) {
        this.processingProgress = Math.min(100, Math.max(0, progress));
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== Enums ====================

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    // ⭐ חדש - שלבי עיבוד מפורטים
    public enum ProcessingStage {
        UPLOADING,          // מעלה לשרת
        EXTRACTING_TEXT,    // מחלץ טקסט מה-PDF
        CREATING_CHUNKS,    // מחלק לחלקים
        CREATING_EMBEDDINGS,// יוצר embeddings
        STORING,            // שומר ב-Qdrant
        COMPLETED,          // הושלם
        FAILED              // נכשל
    }
}