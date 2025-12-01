package com.example.backend.document.mapper;

import com.example.backend.document.dto.DocumentResponse;
import com.example.backend.document.model.Document;
import org.mapstruct.*;

import java.time.Duration;
import java.util.List;

/**
 * Mapper להמרה בין Document Entity ל-DTOs
 */
@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface DocumentMapper {

    // Map Document entity to DTO
    @Mapping(source = "id", target = "id")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "fileSizeFormatted", expression = "java(formatFileSize(document.getFileSize()))")
    @Mapping(target = "processingStageDescription", expression = "java(getStageDescription(document))")
    @Mapping(target = "statistics", ignore = true)
    DocumentResponse toResponse(Document document);

    // Map list of documents
    List<DocumentResponse> toResponseList(List<Document> documents);

    // ==================== Helper Methods ====================

    // Format bytes to human readable
    default String formatFileSize(Long fileSize) {
        if (fileSize == null) {
            return "לא ידוע";
        }

        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // Get processing stage description
    default String getStageDescription(Document document) {
        if (document.getProcessingStage() == null) {
            return "";
        }
        
        return switch (document.getProcessingStage()) {
            case UPLOADING -> "מעלה לשרת...";
            case EXTRACTING_TEXT -> "מחלץ טקסט מהמסמך...";
            case CREATING_CHUNKS -> "מחלק לחלקים...";
            case CREATING_EMBEDDINGS -> "יוצר embeddings...";
            case STORING -> "שומר במאגר...";
            case COMPLETED -> "הושלם בהצלחה";
            case FAILED -> "נכשל";
        };
    }

    // Calculate processing statistics
    default DocumentResponse.ProcessingStatistics buildStatistics(Document document) {
        if (document.getCreatedAt() == null || document.getProcessedAt() == null) {
            return null;
        }

        Duration duration = Duration.between(document.getCreatedAt(), document.getProcessedAt());
        long millis = duration.toMillis();

        return DocumentResponse.ProcessingStatistics.builder()
                .processingTimeMs(millis)
                .processingTimeFormatted(formatDuration(millis))
                .embeddingsCount(document.getChunkCount())
                .estimatedCost(calculateEmbeddingCost(document.getCharacterCount()))
                .build();
    }

    // Format milliseconds to readable time
    default String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        }

        long seconds = millis / 1000;
        if (seconds < 60) {
            return seconds + " שניות";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " דקות";
        }

        long hours = minutes / 60;
        return hours + " שעות";
    }

    // Estimate OpenAI embedding cost
    default Double calculateEmbeddingCost(Integer characterCount) {
        if (characterCount == null) {
            return 0.0;
        }

        // Estimate: 1 token ≈ 4 characters
        int estimatedTokens = characterCount / 4;
        return estimatedTokens * 0.00000013;
    }

    // Add statistics after mapping
    @AfterMapping
    default void enrichDocumentResponse(@MappingTarget DocumentResponse response, Document document) {

        // Add statistics if the document is complete
        if (document.isProcessed()) {
            response.setStatistics(buildStatistics(document));
        }

        // Make sure there is fileSizeFormatted
        if (response.getFileSizeFormatted() == null) {
            response.setFileSizeFormatted(formatFileSize(document.getFileSize()));
        }
    }
}