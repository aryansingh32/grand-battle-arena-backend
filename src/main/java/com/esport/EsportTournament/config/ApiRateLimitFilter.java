package com.esport.EsportTournament.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory fixed-window rate limiter.
 * Intended as a safety layer for local/prod single-node deployments.
 * In multi-node deployments, replace with Redis-based distributed limiting.
 */
@Slf4j
@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final int GENERAL_LIMIT_PER_MIN = 120;
    private static final int SENSITIVE_LIMIT_PER_MIN = 30;
    private static final int PUBLIC_LIMIT_PER_MIN = 90;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || uri.startsWith("/actuator/")
                || uri.startsWith("/ws/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = extractClientIp(request);
        int limit = resolveLimit(path, method);
        String key = ip + "|" + method + "|" + normalizePath(path);

        boolean allowed = allowRequest(key, limit);
        if (!allowed) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests. Please retry later.\"}");
            log.warn("Rate limit exceeded for ip={} path={} method={}", ip, path, method);
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private int resolveLimit(String path, String method) {
        // Public endpoints are typically unauthenticated and can be abused.
        if (path.equals("/api/app/version") || path.equals("/api/filters")
                || path.equals("/api/support") || path.equals("/api/terms")) {
            return PUBLIC_LIMIT_PER_MIN;
        }
        // Transaction and payment-sensitive operations.
        if (path.startsWith("/api/transactions/") || path.startsWith("/api/v1/payments/")
                || path.startsWith("/api/admin/transactions")) {
            return SENSITIVE_LIMIT_PER_MIN;
        }
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            return SENSITIVE_LIMIT_PER_MIN;
        }
        return GENERAL_LIMIT_PER_MIN;
    }

    private String normalizePath(String path) {
        return path.replaceAll("/\\d+", "/{id}");
    }

    private boolean allowRequest(String key, int limit) {
        long now = Instant.now().toEpochMilli();
        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter(now, 0));
        synchronized (counter) {
            if (now - counter.windowStart >= WINDOW_MILLIS) {
                counter.windowStart = now;
                counter.count = 1;
                return true;
            }
            if (counter.count >= limit) {
                return false;
            }
            counter.count++;
            return true;
        }
    }

    private static final class WindowCounter {
        private long windowStart;
        private int count;

        private WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}

