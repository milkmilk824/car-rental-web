package com.example.carrental.config;

import com.example.carrental.security.AuthInterceptor;
import com.example.carrental.security.AuditInterceptor;
import com.example.carrental.security.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final AuditInterceptor auditInterceptor;
    private final String uploadDir;

    public WebMvcConfig(
            AuthInterceptor authInterceptor,
            RateLimitInterceptor rateLimitInterceptor,
            AuditInterceptor auditInterceptor,
            @Value("${app.upload.dir:uploads}") String uploadDir
    ) {
        this.authInterceptor = authInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.auditInterceptor = auditInterceptor;
        this.uploadDir = uploadDir;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/h2-console/**");
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(auditInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
