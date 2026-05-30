package com.example.carrental.security;

import com.example.carrental.common.Enums.UserRole;

public record CurrentUser(Long id, String username, UserRole role) {
}
