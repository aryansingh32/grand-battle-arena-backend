package com.esport.EsportTournament.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket Service for Real-time Broadcasting
 * Handles live updates to connected Flutter clients
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast tournament update to all subscribed users
     * Topic: /topic/tournament/{tournamentId}
     */
    public void broadcastTournamentUpdate(int tournamentId, Map<String, Object> data) {
        try {
            String destination = "/topic/tournament/" + tournamentId;

            // Add timestamp for client-side synchronization
            data.put("timestamp", LocalDateTime.now().toString());
            data.put("type", "tournament_update");

            messagingTemplate.convertAndSend(destination, data);

            log.debug("📡 Broadcast tournament update: tournamentId={}", tournamentId);
        } catch (Exception e) {
            log.error("❌ Failed to broadcast tournament update: tournamentId={}", tournamentId, e);
        }
    }

    /**
     * Broadcast slot booking update to tournament subscribers
     * Topic: /topic/tournament/{tournamentId}/slots
     */
    public void broadcastSlotUpdate(int tournamentId, Map<String, Object> slotData) {
        try {
            String destination = "/topic/tournament/" + tournamentId + "/slots";

            slotData.put("timestamp", LocalDateTime.now().toString());
            slotData.put("type", "slot_update");

            messagingTemplate.convertAndSend(destination, slotData);

            log.debug("📡 Broadcast slot update: tournamentId={}, slot={}",
                    tournamentId, slotData.get("slotNumber"));
        } catch (Exception e) {
            log.error("❌ Failed to broadcast slot update: tournamentId={}", tournamentId, e);
        }
    }

    /**
     * Send personal notification to specific user
     * Queue: /user/{firebaseUID}/queue/notifications
     */
    public void sendUserNotification(String firebaseUID, Map<String, Object> notification) {
        try {
            notification.put("timestamp", LocalDateTime.now().toString());
            notification.put("type", "personal_notification");

            messagingTemplate.convertAndSendToUser(
                    firebaseUID,
                    "/queue/notifications",
                    notification
            );

            log.debug("📨 Sent notification to user: {}", firebaseUID);
        } catch (Exception e) {
            log.error("❌ Failed to send notification to user: {}", firebaseUID, e);
        }
    }

    /**
     * Broadcast wallet update to user
     */
    public void broadcastWalletUpdate(String firebaseUID, int newBalance, String reason) {
        Map<String, Object> walletUpdate = Map.of(
                "balance", newBalance,
                "reason", reason,
                "type", "wallet_update"
        );

        sendUserNotification(firebaseUID, walletUpdate);
    }

    /**
     * Broadcast tournament start notification
     */
    public void broadcastTournamentStart(int tournamentId, String gameId, String gamePassword) {
        Map<String, Object> startData = Map.of(
                "event", "TOURNAMENT_STARTED",
                "gameId", gameId,
                "gamePassword", gamePassword,
                "message", "Tournament has started! Join now with provided credentials."
        );

        broadcastTournamentUpdate(tournamentId, startData);
    }

    /**
     * Broadcast tournament cancellation
     */
    public void broadcastTournamentCancellation(int tournamentId, String reason) {
        Map<String, Object> cancelData = Map.of(
                "event", "TOURNAMENT_CANCELLED",
                "reason", reason,
                "message", "Tournament has been cancelled. Entry fees will be refunded."
        );

        broadcastTournamentUpdate(tournamentId, cancelData);
    }

    /**
     * Broadcast live participant count update
     */
    public void broadcastParticipantCount(int tournamentId, long bookedSlots, long availableSlots) {
        Map<String, Object> countData = Map.of(
                "event", "PARTICIPANT_COUNT_UPDATE",
                "bookedSlots", bookedSlots,
                "availableSlots", availableSlots,
                "totalSlots", bookedSlots + availableSlots
        );

        broadcastTournamentUpdate(tournamentId, countData);
    }

    /**
     * Broadcast global announcement to all users
     */
    public void broadcastGlobalAnnouncement(String title, String message, String priority) {
        try {
            Map<String, Object> announcement = Map.of(
                    "title", title,
                    "message", message,
                    "priority", priority, // LOW, MEDIUM, HIGH, CRITICAL
                    "timestamp", LocalDateTime.now().toString(),
                    "type", "global_announcement"
            );

            messagingTemplate.convertAndSend("/topic/announcements", announcement);

            log.info("📢 Broadcast global announcement: {}", title);
        } catch (Exception e) {
            log.error("❌ Failed to broadcast global announcement", e);
        }
    }

    /**
     * Broadcast maintenance mode notification
     */
    public void broadcastMaintenanceMode(boolean enabled, LocalDateTime scheduledTime, int durationMinutes) {
        Map<String, Object> maintenanceData = Map.of(
                "event", "MAINTENANCE_MODE",
                "enabled", enabled,
                "scheduledTime", scheduledTime != null ? scheduledTime.toString() : null,
                "durationMinutes", durationMinutes,
                "message", enabled ?
                        "System maintenance scheduled. Service will be temporarily unavailable." :
                        "Maintenance completed. System is now operational."
        );

        messagingTemplate.convertAndSend("/topic/system", maintenanceData);
        log.warn("🔧 Broadcast maintenance mode: enabled={}", enabled);
    }
}