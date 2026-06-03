package com.example.carrental.security;

import com.example.carrental.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditService auditService;

    public AuditInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!shouldAudit(request)) {
            return;
        }
        AuthContext.optional().ifPresent(actor -> auditService.record(new AuditService.AuditCommand(
                actor.id(),
                actor.username(),
                actor.role(),
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                ex == null && response.getStatus() < 400,
                clientIp(request),
                request.getHeader("User-Agent"),
                ex == null ? null : ex.getMessage()
        )));
    }

    private boolean shouldAudit(HttpServletRequest request) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }
        String path = request.getRequestURI();
        return path.startsWith("/api/admin/")
                || path.startsWith("/api/store/")
                || "/api/payments/refund".equals(path);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
