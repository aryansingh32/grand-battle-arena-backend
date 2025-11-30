package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.dto.NotificationsDTO;
import com.esport.EsportTournament.model.Notifications;
import com.esport.EsportTournament.service.NotificationService;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    // ===========================================================================
    // Admin Notification Management
    // ===========================================================================

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping
    public ResponseEntity<NotificationsDTO> createNotification(
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String title = (String) request.get("title");
        String message = (String) request.get("message");
        String audienceStr = (String) request.get("targetAudience");

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Notifications.TargetAudience audience = Notifications.TargetAudience.valueOf(audienceStr.toUpperCase());

        NotificationsDTO notification = notificationService.createNotification(title, message, audience, adminUID);
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/user/{firebaseUID}")
    public ResponseEntity<NotificationsDTO> sendNotificationToUser(
            @PathVariable String firebaseUID,
            @Valid @RequestBody Map<String, String> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String title = request.get("title");
        String message = request.get("message");

        NotificationsDTO notification = notificationService.sendNotificationToUser(firebaseUID, title, message, adminUID);
        return ResponseEntity.ok(notification);
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/tournament/{tournamentId}/credentials")
    public ResponseEntity<String> sendTournamentCredentials(
            @PathVariable int tournamentId,
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String gameId = (String) request.get("gameId");
        String gamePassword = (String) request.get("gamePassword");
        List<String> participantUIDs = (List<String>) request.get("participantUIDs");

        if (gameId == null || gamePassword == null || participantUIDs == null || participantUIDs.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        notificationService.sendGameCredentials(tournamentId, gameId, gamePassword, participantUIDs);
        return ResponseEntity.ok("Tournament credentials sent to " + participantUIDs.size() + " participants");
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/tournament/{tournamentId}/reminder")
    public ResponseEntity<String> sendTournamentReminder(
            @PathVariable int tournamentId,
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String tournamentName = (String) request.get("tournamentName");
        Integer minutesBefore = (Integer) request.get("minutesBefore");
        List<String> participantUIDs = (List<String>) request.get("participantUIDs");

        if (tournamentName == null || participantUIDs == null || participantUIDs.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        notificationService.sendTournamentReminder(tournamentId, tournamentName, participantUIDs,
                minutesBefore != null ? minutesBefore : 15);
        return ResponseEntity.ok("Tournament reminder sent to " + participantUIDs.size() + " participants");
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/tournament/{tournamentId}/result")
    public ResponseEntity<String> sendTournamentResult(
            @PathVariable int tournamentId,
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String resultMessage = (String) request.get("resultMessage");
        List<String> participantUIDs = (List<String>) request.get("participantUIDs");

        if (resultMessage == null || participantUIDs == null || participantUIDs.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        notificationService.sendTournamentResult(tournamentId, resultMessage, participantUIDs);
        return ResponseEntity.ok("Tournament result sent to " + participantUIDs.size() + " participants");
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/wallet-transaction")
    public ResponseEntity<String> sendWalletNotification(
            @Valid @RequestBody Map<String, String> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String firebaseUID = request.get("firebaseUID");
        String message = request.get("message");

        if (firebaseUID == null || message == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        notificationService.notifyWalletTransaction(firebaseUID, message, adminUID);
        return ResponseEntity.ok("Wallet notification sent successfully");
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/reward-distribution")
    public ResponseEntity<String> sendRewardNotification(
            @Valid @RequestBody Map<String, String> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String firebaseUID = request.get("firebaseUID");
        String rewardMessage = request.get("rewardMessage");

        if (firebaseUID == null || rewardMessage == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        notificationService.notifyRewardDistribution(firebaseUID, rewardMessage, adminUID);
        return ResponseEntity.ok("Reward notification sent successfully");
    }

    // ===========================================================================
    // Enhanced Notifications
    // ===========================================================================

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/tournament/{tournamentId}/created")
    public ResponseEntity<String> sendTournamentCreated(
            @PathVariable int tournamentId,
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String tournamentName = (String) request.get("tournamentName");
        Integer prizePool = (Integer) request.get("prizePool");
        Integer entryFee = (Integer) request.get("entryFee");
        String startTime = (String) request.get("startTime");

        if (tournamentName == null || prizePool == null || entryFee == null || startTime == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        notificationService.sendTournamentCreatedNotification(tournamentId, tournamentName,
                prizePool, entryFee, startTime);
        return ResponseEntity.ok("Tournament created notification sent to all users");
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/tournament/{tournamentId}/booking-reminder")
    public ResponseEntity<String> sendBookingReminder(
            @PathVariable int tournamentId,
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String tournamentName = (String) request.get("tournamentName");
        Integer remainingSlots = (Integer) request.get("remainingSlots");

        if (tournamentName == null || remainingSlots == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        notificationService.sendBookingReminderNotification(tournamentId, tournamentName, remainingSlots);
        return ResponseEntity.ok("Booking reminder sent to all users");
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/custom")
    public ResponseEntity<String> sendCustomNotification(
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String title = (String) request.get("title");
        String message = (String) request.get("message");
        List<String> targetUserUIDs = (List<String>) request.get("targetUserUIDs");

        if (title == null || title.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Title and message are required");
        }

        notificationService.sendCustomNotification(title, message, targetUserUIDs, adminUID);
        
        String response = targetUserUIDs == null || targetUserUIDs.isEmpty()
                ? "Custom notification sent to all users"
                : String.format("Custom notification sent to %d users", targetUserUIDs.size());
        
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/event")
    public ResponseEntity<String> sendEventNotification(
            @Valid @RequestBody Map<String, String> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String eventTitle = request.get("eventTitle");
        String eventMessage = request.get("eventMessage");
        String eventDate = request.get("eventDate");

        if (eventTitle == null || eventMessage == null || eventDate == null) {
            return ResponseEntity.badRequest().body("All event fields are required");
        }

        notificationService.sendEventNotification(eventTitle, eventMessage, eventDate, adminUID);
        return ResponseEntity.ok("Event notification sent to all users");
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/tournament/{tournamentId}/rules")
    public ResponseEntity<String> sendTournamentRules(
            @PathVariable int tournamentId,
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        List<String> rules = (List<String>) request.get("rules");
        List<String> participantUIDs = (List<String>) request.get("participantUIDs");

        if (rules == null || rules.isEmpty() || participantUIDs == null || participantUIDs.isEmpty()) {
            return ResponseEntity.badRequest().body("Rules and participant list are required");
        }

        notificationService.sendTournamentRulesNotification(tournamentId, rules, participantUIDs);
        return ResponseEntity.ok("Tournament rules sent to " + participantUIDs.size() + " participants");
    }

    // ===========================================================================
    // User Notification Access
    // ===========================================================================

    @GetMapping("/my")
    public ResponseEntity<List<NotificationsDTO>> getMyNotifications(Authentication authentication) {
        String firebaseUID = getAuthenticatedUserUID(authentication);
        List<NotificationsDTO> notifications = notificationService.getNotificationsForUser(firebaseUID);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable int notificationId,
            Authentication authentication) {

        String firebaseUID = getAuthenticatedUserUID(authentication);
        notificationService.markNotificationAsRead(notificationId, firebaseUID);
        return ResponseEntity.noContent().build();
    }

    // ===========================================================================
    // Admin Notification Management
    // ===========================================================================

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @GetMapping("/admin")
    public ResponseEntity<List<NotificationsDTO>> getAdminNotifications(Authentication authentication) {
        String adminUID = getAuthenticatedUserUID(authentication);
        List<NotificationsDTO> notifications = notificationService.getNotificationsForAdmin(adminUID);
        return ResponseEntity.ok(notifications);
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable int notificationId,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        notificationService.deleteNotification(notificationId, adminUID);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        Map<String, Object> stats = notificationService.getNotificationStats();
        return ResponseEntity.ok(stats);
    }

    // ===========================================================================
    // Utility Methods
    // ===========================================================================

    private String getAuthenticatedUserUID(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return (String) authentication.getPrincipal();
    }
}