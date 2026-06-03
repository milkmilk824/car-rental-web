package com.example.carrental.dto;

import com.example.carrental.common.Enums.UserRole;

import java.time.LocalDateTime;

public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditLogResponse(
            Long id,
            Long actorUserId,
            String actorUsername,
            UserRole actorRole,
            String httpMethod,
            String path,
            String action,
            Integer responseStatus,
            Boolean success,
            String clientIp,
            String userAgent,
            String errorMessage,
            LocalDateTime createTime
    ) {
    }
}
