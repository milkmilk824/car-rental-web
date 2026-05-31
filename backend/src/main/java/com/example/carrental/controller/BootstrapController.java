package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.common.BusinessException;
import com.example.carrental.dto.BootstrapDtos;
import com.example.carrental.dto.UserDtos;
import com.example.carrental.security.PublicEndpoint;
import com.example.carrental.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BootstrapController {

    private final UserService userService;
    private final boolean enabled;
    private final String secret;

    public BootstrapController(
            UserService userService,
            @Value("${app.bootstrap.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.secret:}") String secret
    ) {
        this.userService = userService;
        this.enabled = enabled;
        this.secret = secret;
    }

    @PublicEndpoint
    @PostMapping("/api/bootstrap/admin")
    public ApiResponse<UserDtos.LoginResponse> bootstrapAdmin(@Valid @RequestBody BootstrapDtos.BootstrapAdminRequest request) {
        if (!enabled) {
            throw BusinessException.forbidden("Bootstrap 接口未开启");
        }
        return ApiResponse.ok(userService.bootstrapAdmin(request, secret));
    }
}
