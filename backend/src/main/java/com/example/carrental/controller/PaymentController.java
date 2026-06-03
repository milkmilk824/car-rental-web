package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.common.PageResult;
import com.example.carrental.dto.PaymentDtos;
import com.example.carrental.security.AuthContext;
import com.example.carrental.security.PublicEndpoint;
import com.example.carrental.security.RequireRole;
import com.example.carrental.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/payments/create")
    public ApiResponse<PaymentDtos.PaymentResponse> create(
            @Valid @RequestBody PaymentDtos.CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.ok(paymentService.create(request, AuthContext.required().id(), idempotencyKey));
    }

    @GetMapping("/api/payments/status/{paymentNo}")
    public ApiResponse<PaymentDtos.PaymentResponse> status(@PathVariable String paymentNo) {
        return ApiResponse.ok(paymentService.status(paymentNo));
    }

    @PublicEndpoint
    @PostMapping("/api/payments/callback")
    public ApiResponse<PaymentDtos.PaymentResponse> callback(
            @Valid @RequestBody PaymentDtos.PaymentCallbackRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.ok(paymentService.callback(request, idempotencyKey));
    }

    @PostMapping("/api/payments/{paymentNo}/simulate-success")
    public ApiResponse<PaymentDtos.PaymentResponse> simulateSuccess(@PathVariable String paymentNo) {
        return ApiResponse.ok(paymentService.simulateSuccess(paymentNo, AuthContext.required()));
    }

    @RequireRole(UserRole.ADMIN)
    @PostMapping("/api/payments/refund")
    public ApiResponse<PaymentDtos.PaymentResponse> refund(
            @Valid @RequestBody PaymentDtos.RefundRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ApiResponse.ok(paymentService.refund(request, idempotencyKey));
    }

    @GetMapping("/api/payments/order/{orderId}")
    public ApiResponse<PaymentDtos.PaymentResponse> byOrder(@PathVariable Long orderId) {
        return ApiResponse.ok(paymentService.byOrder(orderId, AuthContext.required().id()));
    }

    @RequireRole(UserRole.ADMIN)
    @GetMapping("/api/admin/payments")
    public ApiResponse<PageResult<PaymentDtos.PaymentResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(paymentService.list(page, size));
    }
}
