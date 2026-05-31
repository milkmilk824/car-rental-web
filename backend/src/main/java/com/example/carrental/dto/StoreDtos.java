package com.example.carrental.dto;

import com.example.carrental.common.Enums.StoreStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class StoreDtos {

    private StoreDtos() {
    }

    public record StoreRequest(
            @NotBlank String storeName,
            @NotBlank String city,
            @NotBlank String address,
            String phone,
            String businessHours,
            @NotNull StoreStatus status
    ) {
    }

    public record StoreResponse(
            Long id,
            String storeName,
            String city,
            String address,
            String phone,
            String businessHours,
            StoreStatus status
    ) {
    }

    public record StoreStaffResponse(Long id, StoreResponse store, UserDtos.UserResponse user) {
    }
}
