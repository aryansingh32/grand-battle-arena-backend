package com.esport.EsportTournament.config;

import com.esport.EsportTournament.service.MetricsService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private final MetricsService metricsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        log.debug("FirebaseAuthFilter processing: {} {}", method, requestURI);

        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestURI, method)) {
            log.debug("Skipping authentication for public endpoint: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        log.debug("Authorization header present: {}", authHeader != null);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String idToken = authHeader.substring(7);
            log.debug("Extracted Firebase token (length: {})", idToken.length());

            try {
                // **CRITICAL: Check if Firebase is initialized**
                if (FirebaseApp.getApps().isEmpty()) {
                    log.error("💥 Firebase app not initialized! Cannot verify token.");
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"error\":\"Authentication service unavailable\",\"details\":\"Firebase not initialized\"}");
                    return;
                }

                // Verify Firebase ID token
                log.debug("Verifying Firebase token");
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
                String firebaseUID = decodedToken.getUid();
                String email = decodedToken.getEmail();

                log.debug("Successfully authenticated Firebase user: {} ({})", firebaseUID, email);

                // Create authentication object with Firebase UID as principal
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        firebaseUID,
                        null,
                        Collections.emptyList() // Empty authorities for now, will be set by RoleInjectionFilter
                );

                authentication.setDetails(decodedToken);

                // Store in request attributes for downstream use
                request.setAttribute("firebaseUid", firebaseUID);
                request.setAttribute("firebaseEmail", email);
                request.setAttribute("firebaseToken", decodedToken);
                MDC.put("userId", firebaseUID);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Set authentication in SecurityContext for user: {}", firebaseUID);

            } catch (FirebaseAuthException e) {
                metricsService.recordSignInFailed("invalid_token");
                log.warn("Invalid Firebase token: {} - Error code: {}", e.getMessage(), e.getErrorCode());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter()
                        .write("{\"error\":\"Invalid or expired token\",\"details\":\"" + e.getMessage() + "\"}");
                return;
            } catch (IllegalStateException e) {
                metricsService.recordSignInFailed("firebase_not_initialized");
                // This catches the "FirebaseApp with name [DEFAULT] doesn't exist" error
                log.error("Firebase not initialized: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Authentication service unavailable\",\"details\":\"Firebase initialization failed\"}");
                return;
            } catch (Exception e) {
                metricsService.recordSignInFailed("auth_filter_error");
                log.error("Error processing Firebase token", e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter()
                        .write("{\"error\":\"Authentication failed\",\"details\":\"" + e.getMessage() + "\"}");
                return;
            }
        } else {
            log.debug("No valid Bearer token found for protected endpoint: {}", requestURI);
            // Don't return error here, let Spring Security handle it
        }

        log.debug("Proceeding to next filter for: {}", requestURI);
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String uri, String method) {
        // Specific checks for endpoints that are only public for certain methods
        if (uri.equals("/api/banners")) {
            return "GET".equalsIgnoreCase(method);
        }

        boolean isPublic = uri.startsWith("/api/public/") ||
                uri.equals("/actuator/health") ||
                uri.startsWith("/actuator/") ||
                uri.startsWith("/ws/") ||
                uri.equals("/api/filters") ||
                uri.equals("/api/support") ||
                uri.equals("/api/terms") ||
                uri.equals("/api/v1/payments/amounts") ||
                uri.equals("/api/v1/payments/health");

        if (uri.equals("/api/app/version")) {
            return "GET".equalsIgnoreCase(method);
        }
        if (uri.startsWith("/api/v1/payments/qr")) {
            return "GET".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method);
        }

        log.debug("🌐 Endpoint {} ({}) is public: {}", uri, method, isPublic);
        return isPublic;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean skip = "OPTIONS".equals(request.getMethod());
        if (skip) {
            log.debug("⏭️  Skipping filter for OPTIONS request");
        }
        return skip;
    }
}
