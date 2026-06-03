package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.dto.UserDtos;
import com.example.carrental.security.AuthContext;
import com.example.carrental.security.PublicEndpoint;
import com.example.carrental.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PublicEndpoint
    @PostMapping("/register")
    public ApiResponse<UserDtos.LoginResponse> register(@Valid @RequestBody UserDtos.RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @PublicEndpoint
    @PostMapping("/login")
    public ApiResponse<UserDtos.LoginResponse> login(@Valid @RequestBody UserDtos.LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @PublicEndpoint
    @PostMapping("/refresh")
    public ApiResponse<UserDtos.LoginResponse> refresh(@Valid @RequestBody UserDtos.RefreshTokenRequest request) {
        return ApiResponse.ok(userService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) UserDtos.LogoutRequest request
    ) {
        userService.logout(authorization, request);
        return ApiResponse.ok();
    }

    @GetMapping("/profile")
    public ApiResponse<UserDtos.UserResponse> profile() {
        return ApiResponse.ok(userService.profile(AuthContext.required().id()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserDtos.UserResponse> updateProfile(@RequestBody UserDtos.UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(AuthContext.required().id(), request));
    }

    @PostMapping("/license")
    public ApiResponse<UserDtos.UserResponse> updateLicense(@Valid @RequestBody UserDtos.LicenseRequest request) {
        return ApiResponse.ok(userService.updateLicense(AuthContext.required().id(), request));
    }
}
