package com.esport.EsportTournament.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final FirebaseAuthFilter firebaseAuthFilter;
    private final RoleInjectionFilter roleInjectionFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Add custom filters to the chain
                .addFilterBefore(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(roleInjectionFilter, FirebaseAuthFilter.class)

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/banners").permitAll()
                        .requestMatchers("/api/filters").permitAll()
                        .requestMatchers("/api/app/version").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // Registration endpoints - allow unregistered users to complete registration
                        .requestMatchers("/api/users/me").permitAll()
                        .requestMatchers("/api/users/complete-registration").permitAll()

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
                        .anyRequest().authenticated()
                )
                .build();
    }

    /**
     * Disable automatic registration of our custom filters to prevent conflicts
     * Spring Security will handle them through the filter chain
     */
    // @Bean
    // public FilterRegistrationBean<FirebaseAuthFilter> firebaseAuthFilterRegistration(FirebaseAuthFilter filter) {
    //     FilterRegistrationBean<FirebaseAuthFilter> registration = new FilterRegistrationBean<>(filter);
    //     registration.setEnabled(false); // Disable automatic registration
    //     return registration;
    // }

    // @Bean
    // public FilterRegistrationBean<RoleInjectionFilter> roleInjectionFilterRegistration(RoleInjectionFilter filter) {
    //     FilterRegistrationBean<RoleInjectionFilter> registration = new FilterRegistrationBean<>(filter);
    //     registration.setEnabled(false); // Disable automatic registration
    //     return registration;
    // }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Use setAllowedOriginPatterns to allow wildcards
        config.setAllowedOriginPatterns(List.of(
                // Existing environment variable and localhost for development
                System.getenv().getOrDefault("FRONTEND_ORIGIN", "http://localhost:3000"),
                "http://localhost:8081",
                // This pattern allows any subdomain from ngrok-free.app
                "https://*.ngrok-free.app"
        ));

        // Keep the rest of your configuration as it is
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "ngrok-skip-browser-warning"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
