package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.dto.UploadDtos;
import com.example.carrental.security.RequireRole;
import com.example.carrental.service.UploadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @RequireRole(UserRole.ADMIN)
    @PostMapping("/api/admin/upload/car-image")
    public ApiResponse<UploadDtos.UploadResponse> uploadCarImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(uploadService.uploadCarImage(file));
    }
}
