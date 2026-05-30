package com.example.carrental.dto;

import com.example.carrental.common.Enums.MaintenanceType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class MaintenanceDtos {

    private MaintenanceDtos() {
    }

    public record MaintenanceRequest(
            @NotNull Long carId,
            @NotNull MaintenanceType type,
            String description,
            BigDecimal cost,
            LocalDateTime recordTime
    ) {
    }

    public record MaintenanceResponse(
            Long id,
            Long carId,
            MaintenanceType type,
            String description,
            BigDecimal cost,
            LocalDateTime recordTime
    ) {
    }
}
