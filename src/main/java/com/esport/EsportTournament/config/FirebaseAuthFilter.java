package com.esport.EsportTournament.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FirebaseAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        log.info("🔍 FirebaseAuthFilter processing: {} {}", method, requestURI);

        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestURI)) {
            log.info("✅ Skipping authentication for public endpoint: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        log.info("🔐 Authorization header present: {}", authHeader != null ? "YES" : "NO");

        if (authHeader != null) {
            log.debug("📝 Full Authorization header: {}", authHeader);
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String idToken = authHeader.substring(7);
            log.info("🎫 Extracted Firebase token (length: {})", idToken.length());
            log.debug("🎫 Token preview: {}...", idToken.substring(0, Math.min(50, idToken.length())));

            try {
                // Verify Firebase ID token
                log.info("🔍 Verifying Firebase token...");
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
                String firebaseUID = decodedToken.getUid();
                String email = decodedToken.getEmail();

                log.info("✅ Successfully authenticated Firebase user: {} ({})", firebaseUID, email);

                // Create authentication object with Firebase UID as principal
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                firebaseUID,
                                null,
                                Collections.emptyList() // Empty authorities for now, will be set by RoleInjectionFilter
                        );

                authentication.setDetails(decodedToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("🔒 Set authentication in SecurityContext for user: {}", firebaseUID);

            } catch (FirebaseAuthException e) {
                log.error("❌ Invalid Firebase token: {} - Error code: {}", e.getMessage(), e.getErrorCode());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid or expired token\",\"details\":\"" + e.getMessage() + "\"}");
                return;
            } catch (Exception e) {
                log.error("💥 Error processing Firebase token", e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Authentication failed\",\"details\":\"" + e.getMessage() + "\"}");
                return;
            }
        } else {
            log.warn("⚠️  No valid Bearer token found for protected endpoint: {}", requestURI);
            // Don't return error here, let Spring Security handle it
        }

        log.info("➡️  Proceeding to next filter for: {}", requestURI);
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String uri) {
        boolean isPublic = uri.startsWith("/api/public/") ||
                uri.equals("/actuator/health") ||
                uri.startsWith("/actuator/");

        log.debug("🌐 Endpoint {} is public: {}", uri, isPublic);
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