package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.dto.PaymentDtos;
import com.example.carrental.security.AuthContext;
import com.example.carrental.security.PublicEndpoint;
import com.example.carrental.security.RequireRole;
import com.example.carrental.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/payments/create")
    public ApiResponse<PaymentDtos.PaymentResponse> create(@Valid @RequestBody PaymentDtos.CreatePaymentRequest request) {
        return ApiResponse.ok(paymentService.create(request, AuthContext.required().id()));
    }

    @GetMapping("/api/payments/status/{paymentNo}")
    public ApiResponse<PaymentDtos.PaymentResponse> status(@PathVariable String paymentNo) {
        return ApiResponse.ok(paymentService.status(paymentNo));
    }

    @PublicEndpoint
    @PostMapping("/api/payments/callback")
    public ApiResponse<PaymentDtos.PaymentResponse> callback(@Valid @RequestBody PaymentDtos.PaymentCallbackRequest request) {
        return ApiResponse.ok(paymentService.callback(request));
    }

    @PostMapping("/api/payments/{paymentNo}/simulate-success")
    public ApiResponse<PaymentDtos.PaymentResponse> simulateSuccess(@PathVariable String paymentNo) {
        return ApiResponse.ok(paymentService.simulateSuccess(paymentNo, AuthContext.required()));
    }

    @RequireRole(UserRole.ADMIN)
    @PostMapping("/api/payments/refund")
    public ApiResponse<PaymentDtos.PaymentResponse> refund(@Valid @RequestBody PaymentDtos.RefundRequest request) {
        return ApiResponse.ok(paymentService.refund(request));
    }

    @GetMapping("/api/payments/order/{orderId}")
    public ApiResponse<PaymentDtos.PaymentResponse> byOrder(@PathVariable Long orderId) {
        return ApiResponse.ok(paymentService.byOrder(orderId, AuthContext.required().id()));
    }

    @RequireRole(UserRole.ADMIN)
    @GetMapping("/api/admin/payments")
    public ApiResponse<List<PaymentDtos.PaymentResponse>> list() {
        return ApiResponse.ok(paymentService.list());
    }
}
