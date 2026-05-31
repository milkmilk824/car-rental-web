package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.dto.StoreDtos;
import com.example.carrental.security.AuthContext;
import com.example.carrental.security.PublicEndpoint;
import com.example.carrental.security.RequireRole;
import com.example.carrental.service.StoreStaffService;
import com.example.carrental.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StoreController {

    private final StoreService storeService;
    private final StoreStaffService storeStaffService;

    public StoreController(StoreService storeService, StoreStaffService storeStaffService) {
        this.storeService = storeService;
        this.storeStaffService = storeStaffService;
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

    @RequireRole(UserRole.ADMIN)
    @PostMapping("/api/admin/stores/{storeId}/staff/{userId}")
    public ApiResponse<StoreDtos.StoreStaffResponse> bindStaff(@PathVariable Long storeId, @PathVariable Long userId) {
        return ApiResponse.ok(storeStaffService.bind(storeId, userId));
    }

    @RequireRole(UserRole.ADMIN)
    @GetMapping("/api/admin/stores/{storeId}/staff")
    public ApiResponse<List<StoreDtos.StoreStaffResponse>> staffByStore(@PathVariable Long storeId) {
        return ApiResponse.ok(storeStaffService.staffByStore(storeId));
    }

    @RequireRole(UserRole.ADMIN)
    @DeleteMapping("/api/admin/stores/{storeId}/staff/{userId}")
    public ApiResponse<Void> unbindStaff(@PathVariable Long storeId, @PathVariable Long userId) {
        storeStaffService.unbind(storeId, userId);
        return ApiResponse.ok();
    }

    @RequireRole({UserRole.ADMIN, UserRole.STORE_STAFF})
    @GetMapping("/api/store/my-stores")
    public ApiResponse<List<StoreDtos.StoreResponse>> myStores() {
        return ApiResponse.ok(storeStaffService.myStores(AuthContext.required().id()));
    }
}
