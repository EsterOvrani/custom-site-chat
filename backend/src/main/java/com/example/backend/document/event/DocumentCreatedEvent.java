package com.example.backend.document.event;

import lombok.Getter;

@Getter
public class DocumentCreatedEvent {
    private final Long documentId;
    private final byte[] fileBytes;
    private final String originalFilename;
    private final String contentType;
    private final long fileSize;
    private final String filePath;
    private final Long userId;
    private final String collectionName;

    public DocumentCreatedEvent(
            Long documentId,
            byte[] fileBytes,
            String originalFilename,
            String contentType,
            long fileSize,
            String filePath,
            Long userId,
            String collectionName) {
        this.documentId = documentId;
        this.fileBytes = fileBytes;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.userId = userId;
        this.collectionName = collectionName;
    }
}