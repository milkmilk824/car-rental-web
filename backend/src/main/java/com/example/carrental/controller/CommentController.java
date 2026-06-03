package com.example.carrental.controller;

import com.example.carrental.common.ApiResponse;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.common.PageResult;
import com.example.carrental.dto.CommentDtos;
import com.example.carrental.security.AuthContext;
import com.example.carrental.security.PublicEndpoint;
import com.example.carrental.security.RequireRole;
import com.example.carrental.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/api/comments")
    public ApiResponse<CommentDtos.CommentResponse> create(@Valid @RequestBody CommentDtos.CommentRequest request) {
        return ApiResponse.ok(commentService.create(AuthContext.required().id(), request));
    }

    @PublicEndpoint
    @GetMapping("/api/comments/car/{carId}")
    public ApiResponse<List<CommentDtos.CommentResponse>> byCar(@PathVariable Long carId) {
        return ApiResponse.ok(commentService.byCar(carId));
    }

    @RequireRole(UserRole.ADMIN)
    @GetMapping("/api/admin/comments")
    public ApiResponse<PageResult<CommentDtos.CommentResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(commentService.listAll(page, size));
    }

    @RequireRole(UserRole.ADMIN)
    @DeleteMapping("/api/admin/comments/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        commentService.remove(id);
        return ApiResponse.ok();
    }
}
