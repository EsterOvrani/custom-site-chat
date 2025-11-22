package com.example.backend.collection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionInfoResponse {
    private String collectionName;
    private String secretKey;
    private String embedCode;
    private LocalDateTime createdAt;
    private Integer documentCount;
    private Long totalSize;
}