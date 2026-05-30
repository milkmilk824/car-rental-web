package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.dto.OrderDtos;
import com.example.carrental.security.AuthContext;
import com.example.carrental.security.RequireRole;
import com.example.carrental.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/orders")
    public ApiResponse<OrderDtos.OrderResponse> create(@Valid @RequestBody OrderDtos.OrderCreateRequest request) {
        return ApiResponse.ok(orderService.create(AuthContext.required().id(), request));
    }

    @GetMapping("/api/orders/my")
    public ApiResponse<List<OrderDtos.OrderResponse>> myOrders() {
        return ApiResponse.ok(orderService.myOrders(AuthContext.required().id()));
    }

    @GetMapping("/api/orders/{id}")
    public ApiResponse<OrderDtos.OrderResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(id, AuthContext.required()));
    }

    @PutMapping("/api/orders/{id}/cancel")
    public ApiResponse<OrderDtos.OrderResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok(orderService.cancel(id, AuthContext.required()));
    }

    @PutMapping("/api/orders/{id}/renew")
    public ApiResponse<OrderDtos.OrderResponse> renew(@PathVariable Long id, @Valid @RequestBody OrderDtos.RenewOrderRequest request) {
        return ApiResponse.ok(orderService.renew(id, AuthContext.required().id(), request.extraDays()));
    }

    @RequireRole(UserRole.ADMIN)
    @GetMapping("/api/admin/orders")
    public ApiResponse<List<OrderDtos.OrderResponse>> allOrders() {
        return ApiResponse.ok(orderService.allOrders());
    }

    @RequireRole({UserRole.ADMIN, UserRole.STORE_STAFF})
    @GetMapping("/api/store/orders")
    public ApiResponse<List<OrderDtos.OrderResponse>> storeOrders(@RequestParam Long storeId) {
        return ApiResponse.ok(orderService.storeOrders(storeId));
    }

    @RequireRole({UserRole.ADMIN, UserRole.STORE_STAFF})
    @PutMapping({"/api/admin/orders/{id}/pickup", "/api/store/orders/{id}/pickup"})
    public ApiResponse<OrderDtos.OrderResponse> confirmPickup(@PathVariable Long id) {
        return ApiResponse.ok(orderService.confirmPickup(id));
    }

    @RequireRole({UserRole.ADMIN, UserRole.STORE_STAFF})
    @PutMapping({"/api/admin/orders/{id}/return", "/api/store/orders/{id}/return"})
    public ApiResponse<OrderDtos.OrderResponse> confirmReturn(@PathVariable Long id) {
        return ApiResponse.ok(orderService.confirmReturn(id));
    }
}
