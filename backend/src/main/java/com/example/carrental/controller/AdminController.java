package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.common.Enums.UserStatus;
import com.example.carrental.dto.StatisticsDtos;
import com.example.carrental.dto.UserDtos;
import com.example.carrental.security.RequireRole;
import com.example.carrental.service.StatisticsService;
import com.example.carrental.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequireRole(UserRole.ADMIN)
public class AdminController {

    private final StatisticsService statisticsService;
    private final UserService userService;

    public AdminController(StatisticsService statisticsService, UserService userService) {
        this.statisticsService = statisticsService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<StatisticsDtos.DashboardResponse> dashboard() {
        return ApiResponse.ok(statisticsService.dashboard());
    }

    @GetMapping("/dashboard/revenue-trend")
    public ApiResponse<List<StatisticsDtos.RevenueTrendResponse>> revenueTrend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(statisticsService.revenueTrend(days));
    }

    @GetMapping("/users")
    public ApiResponse<List<UserDtos.UserResponse>> users() {
        return ApiResponse.ok(userService.listUsers());
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserDtos.UserResponse> userDetail(@PathVariable Long id) {
        return ApiResponse.ok(userService.profile(id));
    }

    @PostMapping("/users")
    public ApiResponse<UserDtos.UserResponse> createUser(@Valid @RequestBody UserDtos.AdminCreateUserRequest request) {
        return ApiResponse.ok(userService.createUser(request));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<UserDtos.UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserDtos.AdminUpdateUserRequest request) {
        return ApiResponse.ok(userService.updateUser(id, request));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<UserDtos.UserResponse> updateStatus(@PathVariable Long id, @RequestParam UserStatus status) {
        return ApiResponse.ok(userService.updateStatus(id, status));
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.ok();
    }
}
