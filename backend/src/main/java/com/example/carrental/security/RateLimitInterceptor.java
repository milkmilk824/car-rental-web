package com.example.carrental.security;

import com.example.carrental.common.BusinessException;
import com.example.carrental.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final boolean enabled;
    private final int loginLimit;
    private final int orderLimit;
    private final int callbackLimit;
    private final int uploadLimit;
    private final Duration window;

    public RateLimitInterceptor(
            RateLimitService rateLimitService,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.login.limit:20}") int loginLimit,
            @Value("${app.rate-limit.order.limit:30}") int orderLimit,
            @Value("${app.rate-limit.callback.limit:120}") int callbackLimit,
            @Value("${app.rate-limit.upload.limit:20}") int uploadLimit,
            @Value("${app.rate-limit.window:PT1M}") Duration window
    ) {
        this.rateLimitService = rateLimitService;
        this.enabled = enabled;
        this.loginLimit = loginLimit;
        this.orderLimit = orderLimit;
        this.callbackLimit = callbackLimit;
        this.uploadLimit = uploadLimit;
        this.window = window;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled) {
            return true;
        }
        Rule rule = ruleFor(request);
        if (rule == null) {
            return true;
        }
        if (!rateLimitService.allow(rule.key(), rule.limit(), window)) {
            throw BusinessException.tooManyRequests("请求过于频繁，请稍后再试");
        }
        return true;
    }

    private Rule ruleFor(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("POST".equals(method) && "/api/user/login".equals(path)) {
            return new Rule("login:" + clientIp(request) + ":" + request.getParameter("username"), loginLimit);
        }
        if ("POST".equals(method) && "/api/orders".equals(path)) {
            return new Rule("order:" + actorKey(request), orderLimit);
        }
        if ("POST".equals(method) && "/api/payments/callback".equals(path)) {
            return new Rule("payment-callback:" + clientIp(request), callbackLimit);
        }
        if ("POST".equals(method) && "/api/admin/upload/car-image".equals(path)) {
            return new Rule("upload:" + actorKey(request), uploadLimit);
        }
        return null;
    }

    private String actorKey(HttpServletRequest request) {
        return AuthContext.optional()
                .map(user -> "user:" + user.id())
                .orElseGet(() -> "ip:" + clientIp(request));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Rule(String key, int limit) {
    }
}
