package com.esport.EsportTournament.config;

import com.esport.EsportTournament.service.MetricsService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that adds request context to every log line via SLF4J MDC.
 *
 * Every request automatically gets:
 * - requestId:  unique UUID for correlation across log lines
 * - httpMethod: GET, POST, PATCH, DELETE
 * - httpPath:   /api/users/me, /api/tournaments, etc.
 * - userId:     Firebase UID extracted from X-User-Id header (if present)
 *
 * Also logs request start/end with duration in milliseconds.
 */
@Slf4j
@Component
@Order(1) // Run before all other filters
@RequiredArgsConstructor
public class RequestLoggingFilter implements Filter {

    private final MetricsService metricsService;

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USER_ID_KEY = "userId";
    private static final String HTTP_METHOD_KEY = "httpMethod";
    private static final String HTTP_PATH_KEY = "httpPath";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        final long startTime = System.currentTimeMillis();

        // Generate or reuse a request ID (client can pass X-Request-Id header)
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8); // short UUID
        }

        // Extract user ID from auth header context (set by security filters)
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            userId = "-";
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        // ── Put context into MDC (available in ALL log lines during this request) ──
        MDC.put(REQUEST_ID_KEY, requestId);
        MDC.put(USER_ID_KEY, userId);
        MDC.put(HTTP_METHOD_KEY, method);
        MDC.put(HTTP_PATH_KEY, path);

        // Add request ID to response headers for client-side correlation
        response.setHeader("X-Request-Id", requestId);

        // Skip logging for health check endpoints (too noisy)
        boolean isHealthCheck = path.contains("/actuator/") || path.equals("/health");

        try {
            if (!isHealthCheck) {
                log.info("→ {} {} (user={})", method, path, userId);
            }

            chain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            if (!isHealthCheck) {
                int status = response.getStatus();

                if (status >= 500) {
                    log.error("← {} {} → {} ({}ms)", method, path, status, duration);
                } else if (status >= 400) {
                    log.warn("← {} {} → {} ({}ms)", method, path, status, duration);
                } else {
                    log.info("← {} {} → {} ({}ms)", method, path, status, duration);
                }

                if (status >= 400) {
                    metricsService.recordApiError(path, status, status >= 500 ? "server_error" : "client_error");
                }
            }

            // ── Clean up MDC to prevent leaking between requests ──
            MDC.remove(REQUEST_ID_KEY);
            MDC.remove(USER_ID_KEY);
            MDC.remove(HTTP_METHOD_KEY);
            MDC.remove(HTTP_PATH_KEY);
        }
    }
}
