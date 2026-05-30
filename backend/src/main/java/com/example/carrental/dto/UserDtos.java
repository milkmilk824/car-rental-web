package com.example.carrental.dto;

import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.common.Enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 6, max = 64) String password,
            String phone,
            String email
    ) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String token, UserResponse user) {
    }

    public record UpdateProfileRequest(String phone, String email, String realName) {
    }

    public record LicenseRequest(@NotBlank String realName, @NotBlank String idCard, @NotBlank String driverLicenseNo) {
    }

    public record AdminCreateUserRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 6, max = 64) String password,
            String phone,
            String email,
            UserRole role,
            UserStatus status
    ) {
    }

    public record AdminUpdateUserRequest(
            String phone,
            String email,
            String realName,
            UserRole role,
            UserStatus status
    ) {
    }

    public record UserResponse(
            Long id,
            String username,
            String phone,
            String email,
            String realName,
            String idCard,
            String driverLicenseNo,
            UserStatus status,
            UserRole role
    ) {
    }
}
