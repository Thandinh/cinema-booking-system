package com.cinema.booking.configuration;

import com.cinema.booking.audit.AdminAuditLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuditLogInterceptor adminAuditLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuditLogInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/admin/audit-logs/**",
                        "/api/v1/payments/vnpay-callback",
                        "/api/v1/payments/momo-return",
                        "/api/v1/payments/momo-ipn",
                        "/api/v1/payments/sepay-webhook");
    }
}
