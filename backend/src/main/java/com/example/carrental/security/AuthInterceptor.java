package com.example.carrental.security;

import com.example.carrental.common.BusinessException;
import com.example.carrental.common.Enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        AuthContext.clear();
        tokenService.resolve(request.getHeader("Authorization")).ifPresent(AuthContext::set);

        boolean isPublic = hasAnnotation(method, PublicEndpoint.class);
        if (!isPublic && AuthContext.optional().isEmpty()) {
            throw BusinessException.unauthorized("请先登录");
        }

        RequireRole role = getAnnotation(method, RequireRole.class);
        if (role != null) {
            UserRole current = AuthContext.required().role();
            boolean allowed = Arrays.asList(role.value()).contains(current);
            if (!allowed) {
                throw BusinessException.forbidden("当前账号无权访问该接口");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private <A extends java.lang.annotation.Annotation> boolean hasAnnotation(HandlerMethod method, Class<A> type) {
        return getAnnotation(method, type) != null;
    }

    private <A extends java.lang.annotation.Annotation> A getAnnotation(HandlerMethod method, Class<A> type) {
        A annotation = AnnotationUtils.findAnnotation(method.getMethod(), type);
        if (annotation != null) {
            return annotation;
        }
        return AnnotationUtils.findAnnotation(method.getBeanType(), type);
    }
}
