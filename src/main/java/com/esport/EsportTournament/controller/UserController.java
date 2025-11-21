package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.dto.UserDTO;
import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.service.NotificationService;
import com.esport.EsportTournament.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final NotificationService notificationService; // ✅ inject notification service

    // ---------------- Authentication Helper ----------------
    private String getAuthenticatedUserUID(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return (String) authentication.getPrincipal(); // Firebase UID
    }

    // ---------------- Current User ----------------
    @PostMapping("/me")
    public ResponseEntity<UserDTO> registerCurrentUser(
            @Valid @RequestBody UserDTO userDTO,
            Authentication authentication) {

        String firebaseUID = getAuthenticatedUserUID(authentication);
        log.info("Registering new user with Firebase UID: {}", firebaseUID);

        userDTO.setFirebaseUserUID(firebaseUID);
        UserDTO createdUser = userService.createUser(userDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        String firebaseUID = getAuthenticatedUserUID(authentication);
        log.debug("Fetching profile for user: {}", firebaseUID);

        UserDTO user = userService.getUserByFirebaseUID(firebaseUID);
        return ResponseEntity.ok(user);
    }

    // ---------------- Admin Endpoints ----------------
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    @GetMapping("/{firebaseUID}")
    public ResponseEntity<UserDTO> getUserByAdmin(@PathVariable String firebaseUID) {
        log.info("Admin fetching user: {}", firebaseUID);
        UserDTO user = userService.getUserByFirebaseUID(firebaseUID);
        return ResponseEntity.ok(user);
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        log.info("Admin fetching all users");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    @PutMapping("/{firebaseUID}/role")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable String firebaseUID,
            @RequestBody Map<String, String> requestBody) {

        String roleStr = requestBody.get("role");
        if (roleStr == null || roleStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Role is required");
        }

        try {
            Users.UserRole role = Users.UserRole.valueOf(roleStr.toUpperCase());
            log.info("Admin updating role for user {} to {}", firebaseUID, role);

            return ResponseEntity.ok(userService.updateUserRole(firebaseUID, role));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleStr + ". Valid roles: USER, ADMIN");
        }
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    @PutMapping("/{firebaseUID}/status")
    public ResponseEntity<UserDTO> updateUserStatus(
            @PathVariable String firebaseUID,
            @RequestBody Map<String, String> requestBody) {

        String statusStr = requestBody.get("status");
        if (statusStr == null || statusStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Status is required");
        }

        try {
            Users.UserStatus status = Users.UserStatus.valueOf(statusStr.toUpperCase());
            log.info("Admin updating status for user {} to {}", firebaseUID, status);

            return ResponseEntity.ok(userService.updateUserStatus(firebaseUID, status));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr + ". Valid statuses: ACTIVE, INACTIVE, BANNED");
        }
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        long totalUsers = userService.getAllUsers().size();
        long activeUsers = userService.getActiveUsersCount();
        long adminUsers = userService.getUsersByRole(Users.UserRole.ADMIN).size();
        long bannedUsers = userService.getUsersByStatus(Users.UserStatus.BANNED).size();

        Map<String, Object> stats = Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "adminUsers", adminUsers,
                "bannedUsers", bannedUsers,
                "inactiveUsers", totalUsers - activeUsers - bannedUsers
        );

        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String username) {
        return ResponseEntity.ok(userService.searchUsersByUsername(username));
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable String role) {
        Users.UserRole userRole = Users.UserRole.valueOf(role.toUpperCase());
        return ResponseEntity.ok(userService.getUsersByRole(userRole));
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<UserDTO>> getUsersByStatus(@PathVariable String status) {
        Users.UserStatus userStatus = Users.UserStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(userService.getUsersByStatus(userStatus));
    }

    // ---------------- Device Token Management ----------------
    @PostMapping("/device-token")
    public ResponseEntity<String> updateDeviceToken(
            @Valid @RequestBody Map<String, String> request,
            Authentication authentication) {

        String firebaseUID = getAuthenticatedUserUID(authentication);
        String deviceToken = request.get("deviceToken");

        if (deviceToken == null || deviceToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Device token cannot be empty");
        }

        try {
            notificationService.updateUserDeviceToken(firebaseUID, deviceToken);
            return ResponseEntity.ok("Device token updated successfully");
        } catch (Exception e) {
            log.error("❌ Error updating device token for user {}: {}", firebaseUID, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update device token");
        }
    }
}
