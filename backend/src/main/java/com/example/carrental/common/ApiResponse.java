package com.example.carrental.common;

import java.time.OffsetDateTime;

public record ApiResponse<T>(int code, String message, T data, OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data, OffsetDateTime.now());
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static ApiResponse<Void> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, OffsetDateTime.now());
    }
}
