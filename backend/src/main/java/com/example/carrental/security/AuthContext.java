package com.example.carrental.security;

import com.example.carrental.common.BusinessException;

import java.util.Optional;

public final class AuthContext {

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(CurrentUser user) {
        CURRENT.set(user);
    }

    public static Optional<CurrentUser> optional() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static CurrentUser required() {
        return optional().orElseThrow(() -> BusinessException.unauthorized("请先登录"));
    }

    public static void clear() {
        CURRENT.remove();
    }
}
