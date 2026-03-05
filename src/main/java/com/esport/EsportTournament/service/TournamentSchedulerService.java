package com.esport.EsportTournament.service;

import com.esport.EsportTournament.dto.SlotsDTO;
import com.esport.EsportTournament.model.Tournaments;
import com.esport.EsportTournament.repository.TournamentRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Automated Tournament Scheduler
 * - Auto-starts tournaments at scheduled time
 * - Sends reminders before tournament start
 * - Auto-completes tournaments after duration
 * - Handles tournament lifecycle management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentSchedulerService {

    private final TournamentRepo tournamentRepo;
    private final SlotService slotService;
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;
    private final com.esport.EsportTournament.util.EncryptionUtil encryptionUtil;
    private final Map<String, LocalDateTime> reminderTracker = new ConcurrentHashMap<>();

    /**
     * Check and send reminders for upcoming tournaments
     * Runs every minute
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    @Transactional
    public void sendTournamentReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime in15Minutes = now.plusMinutes(15);
            LocalDateTime in5Minutes = now.plusMinutes(5);

            // Find tournaments starting in 15 minutes
            List<Tournaments> upcomingIn15 = tournamentRepo.findByStatusAndStartTimeBetween(
                    Tournaments.TournamentStatus.UPCOMING,
                    now,
                    in15Minutes
            );

            for (Tournaments tournament : upcomingIn15) {
                // Check if reminder already sent (use Redis for tracking)
                String reminderKey = "reminder_15:" + tournament.getId();

                if (shouldSendReminder(reminderKey)) {
                    sendReminder(tournament, 15);
                    markReminderSent(reminderKey);
                }
            }

            // Find tournaments starting in 5 minutes
            List<Tournaments> upcomingIn5 = tournamentRepo.findByStatusAndStartTimeBetween(
                    Tournaments.TournamentStatus.UPCOMING,
                    now,
                    in5Minutes
            );

            for (Tournaments tournament : upcomingIn5) {
                String reminderKey = "reminder_5:" + tournament.getId();

                if (shouldSendReminder(reminderKey)) {
                    sendReminder(tournament, 5);
                    markReminderSent(reminderKey);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error sending tournament reminders", e);
        }
    }

    /**
     * Auto-start tournaments at scheduled time
     * Runs every 30 seconds for accuracy
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Transactional
    public void autoStartTournaments() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Find tournaments that should start now
            List<Tournaments> tournamentsToStart = tournamentRepo.findByStatusAndStartTimeBefore(
                    Tournaments.TournamentStatus.UPCOMING,
                    now
            );

            for (Tournaments tournament : tournamentsToStart) {
                try {
                    startTournament(tournament);
                } catch (Exception e) {
                    log.error("❌ Failed to start tournament {}: {}", tournament.getId(), e.getMessage());
                }
            }

            if (!tournamentsToStart.isEmpty()) {
                log.info("🚀 Auto-started {} tournaments", tournamentsToStart.size());
            }

        } catch (Exception e) {
            log.error("❌ Error in auto-start scheduler", e);
        }
    }

    /**
     * Auto-complete tournaments after expected duration
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void autoCompleteTournaments() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime twoHoursAgo = now.minusHours(2);

            // Find ongoing tournaments that started more than 2 hours ago
            List<Tournaments> tournamentsToComplete = tournamentRepo.findByStatusAndStartTimeBefore(
                    Tournaments.TournamentStatus.ONGOING,
                    twoHoursAgo);

            for (Tournaments tournament : tournamentsToComplete) {
                try {
                    completeTournament(tournament);
                } catch (Exception e) {
                    log.error("❌ Failed to complete tournament {}: {}", tournament.getId(), e.getMessage());
                }
            }

            if (!tournamentsToComplete.isEmpty()) {
                log.info("✅ Auto-completed {} tournaments", tournamentsToComplete.size());
            }

        } catch (Exception e) {
            log.error("❌ Error in auto-complete scheduler", e);
        }
    }

    /**
     * Clean up old tournament data
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void cleanupOldTournaments() {
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

            List<Tournaments> oldTournaments = tournamentRepo.findByStatusesAndStartTimeBefore(
                    List.of(
                            Tournaments.TournamentStatus.COMPLETED,
                            Tournaments.TournamentStatus.CANCELLED),
                    thirtyDaysAgo);

            // Archive or clean up old tournaments
            log.info("🧹 Found {} old tournaments for cleanup", oldTournaments.size());

            // In production, archive to separate table instead of deleting

        } catch (Exception e) {
            log.error("❌ Error in cleanup scheduler", e);
        }
    }

    /**
     * Monitor tournament health
     * Runs every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // Every 10 minutes
    @Transactional(readOnly = true)
    public void monitorTournamentHealth() {
        try {
            long upcomingCount = tournamentRepo.countByStatus(Tournaments.TournamentStatus.UPCOMING);
            long ongoingCount = tournamentRepo.countByStatus(Tournaments.TournamentStatus.ONGOING);
            long completedToday = tournamentRepo.countByStatusAndUpdatedAtAfter(
                    Tournaments.TournamentStatus.COMPLETED,
                    LocalDateTime.now().minusDays(1));

            log.info("📊 Tournament Health: {} upcoming, {} ongoing, {} completed today",
                    upcomingCount, ongoingCount, completedToday);

            // Check for stale tournaments (ongoing for too long)
            LocalDateTime sixHoursAgo = LocalDateTime.now().minusHours(6);
            long staleTournaments = tournamentRepo.findByStatusAndStartTimeBefore(
                    Tournaments.TournamentStatus.ONGOING,
                    sixHoursAgo).size();

            if (staleTournaments > 0) {
                log.warn("⚠️ Found {} stale tournaments (ongoing for 6+ hours)", staleTournaments);
            }

        } catch (Exception e) {
            log.error("❌ Error monitoring tournament health", e);
        }
    }

    // ========== Private Helper Methods ==========

    private void startTournament(Tournaments tournament) {
        log.info("🚀 Starting tournament: {} - {}", tournament.getId(), tournament.getName());

        // Update tournament status
        tournament.setStatus(Tournaments.TournamentStatus.ONGOING);
        tournament.setUpdatedAt(LocalDateTime.now());
        tournamentRepo.save(tournament);

        // Get all participants
        List<String> participantUIDs = getParticipantUIDs(tournament.getId());

        if (participantUIDs.isEmpty()) {
            log.warn("⚠️ Tournament {} started with no participants", tournament.getId());
            return;
        }

        // Send game credentials to participants
        String decryptedGameId = encryptionUtil.decrypt(tournament.getGameId());
        String decryptedGamePassword = encryptionUtil.decrypt(tournament.getGamePassword());

        if (decryptedGameId != null && !decryptedGameId.isBlank()
                && decryptedGamePassword != null && !decryptedGamePassword.isBlank()) {
            notificationService.sendGameCredentials(
                    tournament.getId(),
                    decryptedGameId,
                    decryptedGamePassword,
                    participantUIDs
            );

            log.info("📧 Sent game credentials to {} participants", participantUIDs.size());
        }

        // Broadcast via WebSocket
        webSocketService.broadcastTournamentStart(
                tournament.getId(),
                decryptedGameId,
                decryptedGamePassword
        );

        // Send individual notifications to each participant
        for (String participantUID : participantUIDs) {
            Map<String, Object> notification = Map.of(
                    "title", "Tournament Started!",
                    "message", String.format("Tournament '%s' has begun! Check your notifications for game details.",
                            tournament.getName()),
                    "tournamentId", tournament.getId(),
                    "gameId", decryptedGameId != null ? decryptedGameId : "",
                    "gamePassword", decryptedGamePassword != null ? decryptedGamePassword : ""
            );
            webSocketService.sendUserNotification(participantUID, notification);
        }

        log.info("✅ Tournament {} started successfully with {} participants",
                tournament.getId(), participantUIDs.size());
    }

    private void completeTournament(Tournaments tournament) {
        log.info("✅ Completing tournament: {} - {}", tournament.getId(), tournament.getName());

        tournament.setStatus(Tournaments.TournamentStatus.COMPLETED);
        tournament.setUpdatedAt(LocalDateTime.now());
        tournamentRepo.save(tournament);

        List<String> participantUIDs = getParticipantUIDs(tournament.getId());

        // Broadcast completion
        Map<String, Object> completionData = Map.of(
                "event", "TOURNAMENT_COMPLETED",
                "message", "Tournament has been completed. Results will be announced shortly."
        );
        webSocketService.broadcastTournamentUpdate(tournament.getId(), completionData);

        // Notify participants
        String resultMessage = String.format(
                "Tournament '%s' has been completed. Thank you for participating!",
                tournament.getName()
        );
        notificationService.sendTournamentResult(tournament.getId(), resultMessage, participantUIDs);

        log.info("✅ Tournament {} completed with {} participants",
                tournament.getId(), participantUIDs.size());
    }

    private void sendReminder(Tournaments tournament, int minutesBefore) {
        List<String> participantUIDs = getParticipantUIDs(tournament.getId());

        if (participantUIDs.isEmpty()) {
            log.debug("No participants to send reminder for tournament {}", tournament.getId());
            return;
        }

        notificationService.sendTournamentReminder(
                tournament.getId(),
                tournament.getName(),
                participantUIDs,
                minutesBefore
        );

        // Broadcast reminder via WebSocket
        Map<String, Object> reminderData = Map.of(
                "event", "TOURNAMENT_REMINDER",
                "minutesBefore", minutesBefore,
                "message", String.format("Tournament starts in %d minutes! Get ready!", minutesBefore)
        );
        webSocketService.broadcastTournamentUpdate(tournament.getId(), reminderData);

        log.info("⏰ Sent {} minute reminder for tournament {} to {} participants",
                minutesBefore, tournament.getId(), participantUIDs.size());
    }

    private List<String> getParticipantUIDs(int tournamentId) {
        try {
            List<SlotsDTO> bookedSlots = slotService.getSlots(tournamentId).stream()
                    .filter(slot -> slot.getFirebaseUserUID() != null)
                    .collect(Collectors.toList());

            return bookedSlots.stream()
                    .map(SlotsDTO::getFirebaseUserUID)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Error getting participant UIDs for tournament {}", tournamentId, e);
            return List.of();
        }
    }

    private boolean shouldSendReminder(String reminderKey) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sentAt = reminderTracker.get(reminderKey);
        return sentAt == null || sentAt.isBefore(now.minusHours(24));
    }

    private void markReminderSent(String reminderKey) {
        LocalDateTime now = LocalDateTime.now();
        reminderTracker.put(reminderKey, now);
        reminderTracker.entrySet().removeIf(entry -> entry.getValue().isBefore(now.minusHours(24)));
    }
}
