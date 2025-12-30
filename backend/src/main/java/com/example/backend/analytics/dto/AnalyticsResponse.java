package com.example.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> AnalyticsResponse<T> success(T data) {
        return AnalyticsResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> AnalyticsResponse<T> success(String message, T data) {
        return AnalyticsResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> AnalyticsResponse<T> error(String message) {
        return AnalyticsResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}