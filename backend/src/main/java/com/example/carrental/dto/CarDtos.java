package com.example.carrental.dto;

import com.example.carrental.common.Enums.CarStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CarDtos {

    private CarDtos() {
    }

    public record CategoryRequest(@NotBlank String categoryName, String description) {
    }

    public record CategoryResponse(Long id, String categoryName, String description) {
    }

    public record CarRequest(
            @NotBlank String carName,
            @NotBlank String brand,
            @NotBlank String model,
            @NotNull Long categoryId,
            @NotBlank String plateNumber,
            @NotNull Long storeId,
            @NotNull @DecimalMin("0.00") BigDecimal pricePerDay,
            @NotNull @DecimalMin("0.00") BigDecimal deposit,
            @NotNull CarStatus status,
            Integer mileage,
            String description,
            List<String> imageUrls
    ) {
    }

    public record CarStatusRequest(@NotNull CarStatus status) {
    }

    public record CarResponse(
            Long id,
            String carName,
            String brand,
            String model,
            CategoryResponse category,
            String plateNumber,
            StoreDtos.StoreResponse store,
            BigDecimal pricePerDay,
            BigDecimal deposit,
            CarStatus status,
            Integer mileage,
            String description,
            List<String> imageUrls
    ) {
    }

    public record AvailabilityResponse(Long carId, boolean available, String reason) {
    }
}
