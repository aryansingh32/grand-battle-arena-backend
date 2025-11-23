package com.esport.EsportTournament.config;

import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.repository.UsersRepo;
import com.esport.EsportTournament.service.RbacService;
import com.esport.EsportTournament.service.WalletService;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleInjectionFilter extends OncePerRequestFilter {

    private final UsersRepo usersRepo;
    private final WalletService walletService;
    private final RbacService rbacService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.info("👤 RoleInjectionFilter processing: {}", requestURI);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("🔍 Current authentication: {}", 
            authentication != null ? authentication.getClass().getSimpleName() : "Null");
        
        if (authentication != null) {
            log.info("🔍 Authentication principal type: {}", 
                authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getSimpleName() : "Null");
            log.info("🔍 Is authenticated: {}", authentication.isAuthenticated());
        }

        // Only process if we have a Firebase authenticated user
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof String firebaseUID) {
            log.info("🔑 Processing roles for Firebase UID: {}", firebaseUID);

            try {
                // Try to fetch user from database
                usersRepo.findByFirebaseUserUID(firebaseUID).ifPresentOrElse(
                        user -> {
                            log.info("✅ User found in database: {} (Role: {}, Status: {})",
                                    user.getUserName(), user.getRole(), user.getStatus());
                            // User exists, assign roles
                            assignUserRoles(authentication, user, firebaseUID);
                        },
                        () -> {
                            // User doesn't exist, create automatically
                            log.info("❌ User not found in database, creating new user for Firebase UID: {}", firebaseUID);
                            try {
                                createNewUserFromFirebaseToken(authentication, firebaseUID);
                            } catch (Exception e) {
                                log.error("💥 Failed to auto-create user for Firebase UID: {}", firebaseUID, e);
                                // Continue with minimal authentication - user can access public endpoints
                                // but will need to complete registration manually
                                assignMinimalRole(authentication, firebaseUID);
                            }
                        }
                );

            } catch (Exception e) {
                log.error("💥 Error during role injection for user: {}", firebaseUID, e);
                // Don't fail the request, just log the error
            }
        } else {
            log.info("⚠️  No Firebase authentication found or not authenticated, skipping role injection");
        }

        log.info("➡️  Role injection complete, proceeding to next filter");
        filterChain.doFilter(request, response);
    }

    /**
     * Auto-create user from Firebase token information
     */
    @Transactional
    private void createNewUserFromFirebaseToken(org.springframework.security.core.Authentication authentication, String firebaseUID) {
        try {
            log.info("🏗️  Creating new user from Firebase token for UID: {}", firebaseUID);

            // Get Firebase token details if available
            FirebaseToken firebaseToken = (FirebaseToken) authentication.getDetails();

            String email = null;
            String displayName = null;

            if (firebaseToken != null) {
                email = firebaseToken.getEmail();
                displayName = firebaseToken.getName();
                log.info("📧 Firebase token details - Email: {}, Name: {}", email, displayName);
            }

            // Create new user with default values
            Users newUser = new Users();
            newUser.setFirebaseUserUID(firebaseUID);
            newUser.setEmail(email != null ? email : "user_" + firebaseUID.substring(0, 8) + "@example.com");
            newUser.setUserName(displayName != null ? displayName : "User_" + firebaseUID.substring(0, 8));
            newUser.setRole(Users.UserRole.USER); // Default role
            newUser.setStatus(Users.UserStatus.ACTIVE); // Default status
            newUser.setCreatedAt(LocalDateTime.now());

            // Handle potential duplicates gracefully
            try {
                Users savedUser = usersRepo.save(newUser);
                rbacService.ensureDefaultRole(savedUser);
                log.info("✅ Auto-created user successfully: ID={}, Email={}", savedUser.getId(), savedUser.getEmail());

                // Create wallet for new user
                try {
                    walletService.createWalletForUser(firebaseUID);
                    log.info("💰 Auto-created wallet for new user: {}", firebaseUID);
                } catch (Exception walletError) {
                    log.warn("⚠️  Failed to create wallet for auto-created user: {}", firebaseUID, walletError);
                }

                // Assign roles to the newly created user
                assignUserRoles(authentication, savedUser, firebaseUID);

            } catch (Exception dbError) {
                log.error("💥 Database error while creating user: {}", firebaseUID, dbError);
                // If user creation fails, try to find if user was created by another thread
                usersRepo.findByFirebaseUserUID(firebaseUID).ifPresentOrElse(
                        user -> {
                            log.info("🔄 User found after creation attempt, assigning roles: {}", firebaseUID);
                            assignUserRoles(authentication, user, firebaseUID);
                        },
                        () -> {
                            log.warn("⚠️  User creation failed and user not found, assigning minimal role: {}", firebaseUID);
                            assignMinimalRole(authentication, firebaseUID);
                        }
                );
            }
        } catch (Exception e) {
            log.error("💥 Unexpected error during user auto-creation: {}", firebaseUID, e);
            assignMinimalRole(authentication, firebaseUID);
        }
    }

    /**
     * Assign proper roles to existing user
     */
    private void assignUserRoles(org.springframework.security.core.Authentication authentication, Users user, String firebaseUID) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        rbacService.getGrantedAuthorities(firebaseUID)
                .forEach(auth -> authorities.add(new SimpleGrantedAuthority(auth)));

        // Check if user is banned
        if (user.getStatus() == Users.UserStatus.BANNED) {
            log.warn("🚫 Banned user attempting access: {}", firebaseUID);
            // Still allow authentication but with limited access
            authorities.clear();
            authorities.add(new SimpleGrantedAuthority("ROLE_BANNED"));
        }

        log.info("🎭 Assigned authorities {} to user {}", authorities, firebaseUID);

        // Update authentication with roles
        var updatedAuth = new UsernamePasswordAuthenticationToken(
                firebaseUID,
                authentication.getCredentials(),
                authorities
        );
        updatedAuth.setDetails(authentication.getDetails());

        SecurityContextHolder.getContext().setAuthentication(updatedAuth);
        log.info("🔒 Updated SecurityContext with roles for user: {}", firebaseUID);
    }

    /**
     * Assign minimal role when user creation fails
     */
    private void assignMinimalRole(org.springframework.security.core.Authentication authentication, String firebaseUID) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_UNREGISTERED"));

        var updatedAuth = new UsernamePasswordAuthenticationToken(
                firebaseUID,
                authentication.getCredentials(),
                authorities
        );
        updatedAuth.setDetails(authentication.getDetails());

        SecurityContextHolder.getContext().setAuthentication(updatedAuth);

        log.info("🔓 Assigned minimal role (UNREGISTERED) to user: {}", firebaseUID);
    }

    /**
     * Skip role injection for public endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean shouldSkip = uri.startsWith("/api/public/") ||
                uri.equals("/actuator/health") ||
                uri.startsWith("/actuator/") ||
                uri.startsWith("/ws/") ||
                "OPTIONS".equals(request.getMethod());

        if (shouldSkip) {
            log.debug("⏭️  Skipping role injection for: {}", uri);
        }

        return shouldSkip;
    }
}
