package com.example.carrental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class BootstrapDtos {

    private BootstrapDtos() {
    }

    public record BootstrapAdminRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 6, max = 64) String password,
            String phone,
            String email,
            @NotBlank String secret
    ) {
    }
}
