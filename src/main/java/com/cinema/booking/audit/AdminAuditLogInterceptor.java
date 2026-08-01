package com.cinema.booking.audit;

import com.cinema.booking.service.AdminAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminAuditLogInterceptor implements HandlerInterceptor {

    private final AdminAuditLogService adminAuditLogService;

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        adminAuditLogService.record(request, response, exception);
    }
}
