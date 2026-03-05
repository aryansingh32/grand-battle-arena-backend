package com.esport.EsportTournament.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiRateLimitFilter apiRateLimitFilter;
    private final FirebaseAuthFilter firebaseAuthFilter;
    private final RoleInjectionFilter roleInjectionFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(contentType -> {
                        })
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)))

                // Add custom filters to the chain
                .addFilterBefore(apiRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(firebaseAuthFilter, ApiRateLimitFilter.class)
                .addFilterAfter(roleInjectionFilter, FirebaseAuthFilter.class)

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/banners").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/filters").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/support").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/terms").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/app/version").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/amounts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/qr/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/qr").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        // Registration endpoints - allow unregistered users to complete registration
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/users/complete-registration").authenticated()

                        // Basic authenticated endpoints - all registered users
                        .requestMatchers("/api/tournaments").authenticated()
                        .requestMatchers("/api/tournaments/*/summary").authenticated()
                        .requestMatchers("/api/tournaments/*/").authenticated()
                        .requestMatchers("/api/tournaments/status/*").authenticated()

                        // User-specific endpoints - require USER role or higher
                        .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/wallets/my").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/slots/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/transactions/deposit").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/transactions/withdraw").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/transactions/history").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/transactions/*/cancel").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/notifications/my").hasAnyRole("USER", "ADMIN")

                        // Admin endpoints rely on method-level security; ensure authentication
                        .requestMatchers("/api/admin/**").authenticated()

                        // Banned users - very limited access
                        .requestMatchers("/api/banned/**").hasRole("BANNED")
                        .requestMatchers("/api/users/appeal").hasRole("BANNED")

                        // All other requests require authentication
                        .anyRequest().authenticated())
                .build();
    }

    /**
     * Disable automatic registration of our custom filters to prevent double
     * execution
     */
    @Bean
    public FilterRegistrationBean<FirebaseAuthFilter> firebaseAuthFilterRegistration(FirebaseAuthFilter filter) {
        FilterRegistrationBean<FirebaseAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RoleInjectionFilter> roleInjectionFilterRegistration(RoleInjectionFilter filter) {
        FilterRegistrationBean<RoleInjectionFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        String allowedOriginsEnv = System.getenv().getOrDefault(
                "ALLOWED_ORIGIN_PATTERNS",
                "http://localhost:3000,http://localhost:5173,http://localhost:8081,http://127.0.0.1:8081,http://10.0.2.2:8081");
        List<String> allowedOrigins = Arrays.stream(allowedOriginsEnv.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toList());
        boolean containsWildcard = allowedOrigins.stream().anyMatch(o -> o.equals("*"));

        config.setAllowedOriginPatterns(allowedOrigins);

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*", "Authorization", "Content-Type", "ngrok-skip-browser-warning"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        config.setAllowCredentials(!containsWildcard);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
