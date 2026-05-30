package com.example.carrental.dto;

import com.example.carrental.common.Enums.OrderStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderCreateRequest(
            @NotNull Long carId,
            @NotNull Long pickupStoreId,
            @NotNull Long returnStoreId,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime
    ) {
    }

    public record RenewOrderRequest(@Min(1) int extraDays) {
    }

    public record OrderResponse(
            Long id,
            String orderNo,
            UserDtos.UserResponse user,
            CarDtos.CarResponse car,
            StoreDtos.StoreResponse pickupStore,
            StoreDtos.StoreResponse returnStore,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer rentalDays,
            BigDecimal totalAmount,
            BigDecimal depositAmount,
            OrderStatus status
    ) {
    }
}
