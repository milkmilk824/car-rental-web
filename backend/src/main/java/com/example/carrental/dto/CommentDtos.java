package com.example.carrental.dto;

import com.example.carrental.common.Enums.CommentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class CommentDtos {

    private CommentDtos() {
    }

    public record CommentRequest(
            @NotNull Long orderId,
            @NotNull @Min(1) @Max(5) Integer score,
            String content
    ) {
    }

    public record CommentResponse(
            Long id,
            Long userId,
            String username,
            Long carId,
            Long orderId,
            Integer score,
            String content,
            CommentStatus status,
            LocalDateTime createTime
    ) {
    }
}
