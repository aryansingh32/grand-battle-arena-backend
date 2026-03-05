package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.dto.UserDTO;
import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.model.TournamentResult;
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
import org.hibernate.Hibernate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

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

    @PostMapping("/complete-registration")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> completeRegistration(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        String firebaseUID = getAuthenticatedUserUID(authentication);
        String preferredUsername = request.get("userName");
        String email = request.get("email");
        UserDTO updatedUser = userService.completeUserRegistration(firebaseUID, preferredUsername, email);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        String firebaseUID = getAuthenticatedUserUID(authentication);
        log.debug("Fetching profile for user: {}", firebaseUID);

        UserDTO user = userService.getUserByFirebaseUID(firebaseUID);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me/history")
    public ResponseEntity<List<Map<String, Object>>> getCurrentUserHistory(
            Authentication authentication) {
        String firebaseUID = getAuthenticatedUserUID(authentication);
        log.debug("Fetching history for user: {}", firebaseUID);
        return ResponseEntity.ok(
                userService.getUserHistory(firebaseUID).stream()
                        .map(this::toHistoryResponse)
                        .collect(Collectors.toList()));
    }

    @GetMapping("/{firebaseUID}/history")
    public ResponseEntity<List<Map<String, Object>>> getUserHistory(
            @PathVariable String firebaseUID) {
        log.debug("Fetching history for user: {}", firebaseUID);
        return ResponseEntity.ok(
                userService.getUserHistory(firebaseUID).stream()
                        .map(this::toHistoryResponse)
                        .collect(Collectors.toList()));
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
            throw new IllegalArgumentException(
                    "Invalid status: " + statusStr + ". Valid statuses: ACTIVE, INACTIVE, BANNED");
        }
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    @GetMapping("/stats")
    public ResponseEntity<com.esport.EsportTournament.dto.UserStatsDTO> getUserStats() {
        long totalUsers = userService.getAllUsers().size();
        long activeUsers = userService.getActiveUsersCount();
        long adminUsers = userService.getUsersByRole(Users.UserRole.ADMIN).size();
        long bannedUsers = userService.getUsersByStatus(Users.UserStatus.BANNED).size();

        com.esport.EsportTournament.dto.UserStatsDTO stats = new com.esport.EsportTournament.dto.UserStatsDTO();
        stats.setTotalUsers(totalUsers);
        stats.setActiveUsers(activeUsers);
        stats.setAdminUsers(adminUsers);
        stats.setBannedUsers(bannedUsers);
        stats.setInactiveUsers(totalUsers - activeUsers - bannedUsers);

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

    private Map<String, Object> toHistoryResponse(TournamentResult result) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", result.getId());
        item.put("firebaseUserUID", result.getFirebaseUserUID());
        item.put("playerName", result.getPlayerName());
        item.put("teamName", result.getTeamName());
        item.put("kills", result.getKills());
        item.put("placement", result.getPlacement());
        item.put("coinsEarned", result.getCoinsEarned());
        item.put("createdAt", result.getCreatedAt());

        if (result.getTournament() != null) {
            // Ensure LAZY proxy is initialized before reading fields.
            Hibernate.initialize(result.getTournament());
            item.put("tournamentId", result.getTournament().getId());
            item.put("tournamentName", result.getTournament().getName());
            item.put("game", result.getTournament().getGame());
            item.put("map", result.getTournament().getMapType());
            item.put("startTime", result.getTournament().getStartTime());
            item.put("tournamentStatus", result.getTournament().getStatus());
        } else {
            item.put("tournamentId", null);
        }

        return item;
    }
}
