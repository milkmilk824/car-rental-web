package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.common.PageResult;
import com.example.carrental.dto.ContractDtos;
import com.example.carrental.security.AuthContext;
import com.example.carrental.security.RequireRole;
import com.example.carrental.service.ContractService;
import com.example.carrental.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class ContractController {

    private final ContractService contractService;
    private final OrderService orderService;

    public ContractController(ContractService contractService, OrderService orderService) {
        this.contractService = contractService;
        this.orderService = orderService;
    }

    @RequireRole(UserRole.ADMIN)
    @PostMapping("/api/contracts/generate")
    public ApiResponse<ContractDtos.ContractResponse> generate(@Valid @RequestBody ContractDtos.GenerateContractRequest request) {
        return ApiResponse.ok(contractService.generate(request.orderId()));
    }

    @RequireRole(UserRole.ADMIN)
    @GetMapping("/api/admin/contracts")
    public ApiResponse<PageResult<ContractDtos.ContractResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(contractService.listAll(page, size));
    }

    @RequireRole(UserRole.ADMIN)
    @GetMapping("/api/contracts/{id}")
    public ApiResponse<ContractDtos.ContractResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(contractService.detail(id));
    }

    @GetMapping("/api/contracts/order/{orderId}")
    public ApiResponse<ContractDtos.ContractResponse> byOrder(@PathVariable Long orderId) {
        orderService.detail(orderId, AuthContext.required());
        return ApiResponse.ok(contractService.byOrder(orderId));
    }

    @RequireRole(UserRole.ADMIN)
    @PutMapping("/api/contracts/{id}/sign")
    public ApiResponse<ContractDtos.ContractResponse> sign(@PathVariable Long id) {
        return ApiResponse.ok(contractService.sign(id));
    }
}
