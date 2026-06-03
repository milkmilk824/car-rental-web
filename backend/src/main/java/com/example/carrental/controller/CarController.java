package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.common.Enums.CarStatus;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.common.PageResult;
import com.example.carrental.dto.CarDtos;
import com.example.carrental.dto.MaintenanceDtos;
import com.example.carrental.security.PublicEndpoint;
import com.example.carrental.security.RequireRole;
import com.example.carrental.service.CarService;
import com.example.carrental.service.MaintenanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class CarController {

    private final CarService carService;
    private final MaintenanceService maintenanceService;

    public CarController(CarService carService, MaintenanceService maintenanceService) {
        this.carService = carService;
        this.maintenanceService = maintenanceService;
    }

    @PublicEndpoint
    @GetMapping({"/api/cars", "/api/cars/search"})
    public ApiResponse<PageResult<CarDtos.CarResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) CarStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(carService.search(keyword, brand, categoryId, storeId, city, status, minPrice, maxPrice, page, size));
    }

    @PublicEndpoint
    @GetMapping("/api/cars/categories")
    public ApiResponse<List<CarDtos.CategoryResponse>> categories() {
        return ApiResponse.ok(carService.categories());
    }

    @PublicEndpoint
    @GetMapping("/api/cars/{id}")
    public ApiResponse<CarDtos.CarResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(carService.detail(id));
    }

    @PublicEndpoint
    @GetMapping("/api/cars/{id}/availability")
    public ApiResponse<CarDtos.AvailabilityResponse> availability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        return ApiResponse.ok(carService.availability(id, startTime, endTime));
    }

    @RequireRole(UserRole.ADMIN)
    @PostMapping("/api/admin/cars/categories")
    public ApiResponse<CarDtos.CategoryResponse> createCategory(@Valid @RequestBody CarDtos.CategoryRequest request) {
        return ApiResponse.ok(carService.createCategory(request));
    }

    @RequireRole(UserRole.ADMIN)
    @PutMapping("/api/admin/cars/categories/{id}")
    public ApiResponse<CarDtos.CategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CarDtos.CategoryRequest request) {
        return ApiResponse.ok(carService.updateCategory(id, request));
    }

    @RequireRole(UserRole.ADMIN)
    @DeleteMapping("/api/admin/cars/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        carService.deleteCategory(id);
        return ApiResponse.ok();
    }

    @RequireRole(UserRole.ADMIN)
    @PostMapping("/api/admin/cars")
    public ApiResponse<CarDtos.CarResponse> create(@Valid @RequestBody CarDtos.CarRequest request) {
        return ApiResponse.ok(carService.create(request));
    }

    @RequireRole(UserRole.ADMIN)
    @PutMapping("/api/admin/cars/{id}")
    public ApiResponse<CarDtos.CarResponse> update(@PathVariable Long id, @Valid @RequestBody CarDtos.CarRequest request) {
        return ApiResponse.ok(carService.update(id, request));
    }

    @RequireRole(UserRole.ADMIN)
    @DeleteMapping("/api/admin/cars/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ApiResponse.ok();
    }

    @RequireRole(UserRole.ADMIN)
    @PutMapping("/api/admin/cars/{id}/status")
    public ApiResponse<CarDtos.CarResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody CarDtos.CarStatusRequest request) {
        return ApiResponse.ok(carService.updateStatus(id, request.status()));
    }

    @RequireRole({UserRole.ADMIN, UserRole.STORE_STAFF})
    @PostMapping("/api/admin/cars/maintenance")
    public ApiResponse<MaintenanceDtos.MaintenanceResponse> createMaintenance(@Valid @RequestBody MaintenanceDtos.MaintenanceRequest request) {
        return ApiResponse.ok(maintenanceService.create(request));
    }

    @RequireRole({UserRole.ADMIN, UserRole.STORE_STAFF})
    @GetMapping("/api/admin/cars/maintenance")
    public ApiResponse<PageResult<MaintenanceDtos.MaintenanceResponse>> maintenanceList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(maintenanceService.listAll(page, size));
    }

    @RequireRole({UserRole.ADMIN, UserRole.STORE_STAFF})
    @GetMapping("/api/admin/cars/{id}/maintenance")
    public ApiResponse<PageResult<MaintenanceDtos.MaintenanceResponse>> maintenance(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(maintenanceService.byCar(id, page, size));
    }

    @RequireRole({UserRole.ADMIN, UserRole.STORE_STAFF})
    @PutMapping("/api/admin/cars/maintenance/{id}")
    public ApiResponse<MaintenanceDtos.MaintenanceResponse> updateMaintenance(@PathVariable Long id, @Valid @RequestBody MaintenanceDtos.MaintenanceRequest request) {
        return ApiResponse.ok(maintenanceService.update(id, request));
    }

    @RequireRole({UserRole.ADMIN, UserRole.STORE_STAFF})
    @DeleteMapping("/api/admin/cars/maintenance/{id}")
    public ApiResponse<Void> deleteMaintenance(@PathVariable Long id) {
        maintenanceService.delete(id);
        return ApiResponse.ok();
    }
}
