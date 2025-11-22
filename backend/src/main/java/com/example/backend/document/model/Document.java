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
@Data  // ⬅️ זה חשוב!
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
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "processing_progress")
    private Integer processingProgress = 0;

    @Column(name = "character_count")
    private Integer characterCount;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "active")
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

    // ==================== Enum ====================

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}