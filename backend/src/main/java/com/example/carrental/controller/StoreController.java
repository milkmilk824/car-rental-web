package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.dto.StoreDtos;
import com.example.carrental.security.PublicEndpoint;
import com.example.carrental.security.RequireRole;
import com.example.carrental.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PublicEndpoint
    @GetMapping("/api/stores")
    public ApiResponse<List<StoreDtos.StoreResponse>> list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean onlyOpen
    ) {
        return ApiResponse.ok(storeService.list(city, onlyOpen));
    }

    @PublicEndpoint
    @GetMapping("/api/stores/{id}")
    public ApiResponse<StoreDtos.StoreResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(storeService.detail(id));
    }

    @RequireRole(UserRole.ADMIN)
    @PostMapping("/api/admin/stores")
    public ApiResponse<StoreDtos.StoreResponse> create(@Valid @RequestBody StoreDtos.StoreRequest request) {
        return ApiResponse.ok(storeService.create(request));
    }

    @RequireRole(UserRole.ADMIN)
    @PutMapping("/api/admin/stores/{id}")
    public ApiResponse<StoreDtos.StoreResponse> update(@PathVariable Long id, @Valid @RequestBody StoreDtos.StoreRequest request) {
        return ApiResponse.ok(storeService.update(id, request));
    }

    @RequireRole(UserRole.ADMIN)
    @DeleteMapping("/api/admin/stores/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        storeService.delete(id);
        return ApiResponse.ok();
    }
}
