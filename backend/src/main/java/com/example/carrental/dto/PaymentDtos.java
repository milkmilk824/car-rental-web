package com.example.carrental.dto;

import com.example.carrental.common.Enums.PayStatus;
import com.example.carrental.common.Enums.PayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record CreatePaymentRequest(@NotNull Long orderId, @NotNull PayType payType) {
    }

    public record PaymentCallbackRequest(
            @NotBlank String paymentNo,
            @NotBlank String transactionNo,
            @NotNull BigDecimal payAmount,
            String signature
    ) {
    }

    public record RefundRequest(@NotBlank String paymentNo, String reason) {
    }

    public record PaymentResponse(
            Long id,
            String paymentNo,
            Long orderId,
            Long userId,
            BigDecimal payAmount,
            PayType payType,
            PayStatus payStatus,
            String transactionNo,
            LocalDateTime payTime
    ) {
    }
}
