package com.esport.EsportTournament.service;

import com.esport.EsportTournament.dto.NotificationsDTO;
import com.esport.EsportTournament.exception.ResourceNotFoundException;
import com.esport.EsportTournament.model.Notifications;
import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.repository.NotificationRepo;
import com.esport.EsportTournament.repository.UsersRepo;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ ENHANCED: Complete Notification Service
 * - Tournament lifecycle notifications
 * - Firebase Cloud Messaging integration
 * - Admin custom notifications
 * - User-specific and broadcast notifications
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedNotificationService {

        private final NotificationRepo notificationRepo;
        private final UsersRepo usersRepo;

        // ==================== TOURNAMENT NOTIFICATIONS ====================

        /**
         * ✅ NEW: Notify when new tournament is created
         */
        @Async
        @Transactional
        public void notifyNewTournament(int tournamentId, String tournamentName,
                        String game, LocalDateTime startTime, int entryFee) {
                String title = "🎮 New Tournament Available!";
                String message = String.format(
                                "New %s tournament '%s' is now open for registration!\n" +
                                                "Entry Fee: ₹%d\nStarts: %s",
                                game, tournamentName, entryFee, formatDateTime(startTime));

                Map<String, String> data = Map.of(
                                "type", "NEW_TOURNAMENT",
                                "tournamentId", String.valueOf(tournamentId),
                                "tournamentName", tournamentName,
                                "game", game,
                                "entryFee", String.valueOf(entryFee));

                broadcastToAllUsers(title, message, data);
        }

        /**
         * ✅ NEW: Notify participants when tournament is about to start
         */
        @Async
        @Transactional
        public void notifyTournamentStartingSoon(int tournamentId, String tournamentName,
                        List<String> participantUIDs, int minutesBefore) {
                String title = "⏰ Tournament Starting Soon!";
                String message = String.format(
                                "Tournament '%s' starts in %d minutes! Get ready!",
                                tournamentName, minutesBefore);

                Map<String, String> data = Map.of(
                                "type", "TOURNAMENT_REMINDER",
                                "tournamentId", String.valueOf(tournamentId),
                                "minutesBefore", String.valueOf(minutesBefore));

                sendBatchNotifications(participantUIDs, title, message, data);
        }

        /**
         * ✅ NEW: Notify participants when tournament starts with credentials
         */
        @Async
        @Transactional
        public void notifyTournamentStarted(int tournamentId, String tournamentName,
                        String gameId, String gamePassword,
                        List<String> participantUIDs) {
                String title = "🚀 Tournament Started!";
                String message = String.format(
                                "Tournament '%s' has begun!\n\n" +
                                                "🎮 Game ID: %s\n" +
                                                "🔐 Password: %s\n\n" +
                                                "Join now and good luck!",
                                tournamentName, gameId, gamePassword);

                Map<String, String> data = Map.of(
                                "type", "TOURNAMENT_STARTED",
                                "tournamentId", String.valueOf(tournamentId),
                                "gameId", gameId,
                                "gamePassword", gamePassword);

                sendBatchNotifications(participantUIDs, title, message, data);
        }

        /**
         * ✅ NEW: Notify when game credentials are updated
         */
        @Async
        @Transactional
        public void notifyCredentialsUpdated(int tournamentId, String tournamentName,
                        String newGameId, String newGamePassword,
                        List<String> participantUIDs) {
                String title = "🔄 Game Credentials Updated!";
                String message = String.format(
                                "Tournament '%s' credentials have been updated!\n\n" +
                                                "🎮 New Game ID: %s\n" +
                                                "🔐 New Password: %s\n\n" +
                                                "Please use the new credentials to join.",
                                tournamentName, newGameId, newGamePassword);

                Map<String, String> data = Map.of(
                                "type", "CREDENTIALS_UPDATED",
                                "tournamentId", String.valueOf(tournamentId),
                                "gameId", newGameId,
                                "gamePassword", newGamePassword);

                sendBatchNotifications(participantUIDs, title, message, data);
        }

        /**
         * ✅ NEW: Notify when tournament is cancelled
         */
        @Async
        @Transactional
        public void notifyTournamentCancelled(int tournamentId, String tournamentName,
                        String reason, List<String> participantUIDs) {
                String title = "❌ Tournament Cancelled";
                String message = String.format(
                                "Tournament '%s' has been cancelled.\n\n" +
                                                "Reason: %s\n\n" +
                                                "Your entry fee has been refunded to your wallet.",
                                tournamentName, reason != null ? reason : "Not specified");

                Map<String, String> data = Map.of(
                                "type", "TOURNAMENT_CANCELLED",
                                "tournamentId", String.valueOf(tournamentId),
                                "reason", reason != null ? reason : "");

                sendBatchNotifications(participantUIDs, title, message, data);
        }

        /**
         * ✅ NEW: Notify when tournament is completed
         */
        @Async
        @Transactional
        public void notifyTournamentCompleted(int tournamentId, String tournamentName,
                        List<String> participantUIDs) {
                String title = "✅ Tournament Completed";
                String message = String.format(
                                "Tournament '%s' has been completed!\n\n" +
                                                "Results will be announced shortly. " +
                                                "Winners will receive their rewards automatically.",
                                tournamentName);

                Map<String, String> data = Map.of(
                                "type", "TOURNAMENT_COMPLETED",
                                "tournamentId", String.valueOf(tournamentId));

                sendBatchNotifications(participantUIDs, title, message, data);
        }

        // ==================== BOOKING NOTIFICATIONS ====================

        /**
         * ✅ NEW: Confirm slot booking to user
         */
        @Async
        @Transactional
        public void notifySlotBooked(String firebaseUID, int tournamentId, String tournamentName,
                        int slotNumber, int entryFee) {
                String title = "✅ Booking Confirmed!";
                String message = String.format(
                                "You've successfully booked slot #%d for tournament '%s'.\n\n" +
                                                "Entry Fee: ₹%d\n" +
                                                "You'll receive game details when the tournament starts.",
                                slotNumber, tournamentName, entryFee);

                Map<String, String> data = Map.of(
                                "type", "SLOT_BOOKED",
                                "tournamentId", String.valueOf(tournamentId),
                                "slotNumber", String.valueOf(slotNumber));

                sendToUser(firebaseUID, title, message, data);
        }

        /**
         * ✅ NEW: Notify when booking is cancelled
         */
        @Async
        @Transactional
        public void notifyBookingCancelled(String firebaseUID, int tournamentId,
                        String tournamentName, int refundAmount) {
                String title = "🔄 Booking Cancelled";
                String message = String.format(
                                "Your booking for tournament '%s' has been cancelled.\n\n" +
                                                "₹%d has been refunded to your wallet.",
                                tournamentName, refundAmount);

                Map<String, String> data = Map.of(
                                "type", "BOOKING_CANCELLED",
                                "tournamentId", String.valueOf(tournamentId),
                                "refundAmount", String.valueOf(refundAmount));

                sendToUser(firebaseUID, title, message, data);
        }

        // ==================== WALLET NOTIFICATIONS ====================

        /**
         * ✅ NEW: Notify on successful deposit
         */
        @Async
        @Transactional
        public void notifyDepositSuccess(String firebaseUID, int amount, int newBalance) {
                String title = "💰 Deposit Successful!";
                String message = String.format(
                                "₹%d has been added to your wallet.\n\n" +
                                                "New Balance: ₹%d",
                                amount, newBalance);

                Map<String, String> data = Map.of(
                                "type", "DEPOSIT_SUCCESS",
                                "amount", String.valueOf(amount),
                                "newBalance", String.valueOf(newBalance));

                sendToUser(firebaseUID, title, message, data);
        }

        /**
         * ✅ NEW: Notify on pending deposit
         */
        @Async
        @Transactional
        public void notifyDepositPending(String firebaseUID, int amount, String transactionId) {
                String title = "⏳ Deposit Pending";
                String message = String.format(
                                "Your deposit of ₹%d is being processed.\n\n" +
                                                "Transaction ID: %s\n" +
                                                "You'll be notified once it's approved.",
                                amount, transactionId);

                Map<String, String> data = Map.of(
                                "type", "DEPOSIT_PENDING",
                                "amount", String.valueOf(amount),
                                "transactionId", transactionId);

                sendToUser(firebaseUID, title, message, data);
        }

        /**
         * ✅ NEW: Notify on withdrawal request
         */
        @Async
        @Transactional
        public void notifyWithdrawalRequested(String firebaseUID, int amount) {
                String title = "📤 Withdrawal Requested";
                String message = String.format(
                                "Your withdrawal request of ₹%d has been received.\n\n" +
                                                "Processing time: 24-48 hours\n" +
                                                "You'll be notified once it's processed.",
                                amount);

                Map<String, String> data = Map.of(
                                "type", "WITHDRAWAL_REQUESTED",
                                "amount", String.valueOf(amount));

                sendToUser(firebaseUID, title, message, data);
        }

        /**
         * ✅ NEW: Notify on withdrawal approval
         */
        @Async
        @Transactional
        public void notifyWithdrawalApproved(String firebaseUID, int amount, int commission) {
                int netAmount = amount - commission;
                String title = "✅ Withdrawal Approved!";
                String message = String.format(
                                "Your withdrawal has been approved!\n\n" +
                                                "Requested: ₹%d\n" +
                                                "Platform Fee (3%%): ₹%d\n" +
                                                "Net Amount: ₹%d\n\n" +
                                                "Funds will be transferred within 24 hours.",
                                amount, commission, netAmount);

                Map<String, String> data = Map.of(
                                "type", "WITHDRAWAL_APPROVED",
                                "amount", String.valueOf(amount),
                                "commission", String.valueOf(commission),
                                "netAmount", String.valueOf(netAmount));

                sendToUser(firebaseUID, title, message, data);
        }

        // ==================== ADMIN CUSTOM NOTIFICATIONS ====================

        /**
         * ✅ NEW: Send custom notification to specific users
         */
        @Async
        @Transactional
        public void sendCustomNotification(String title, String message,
                        List<String> targetUserUIDs, String adminUID) {
                log.info("Admin {} sending custom notification to {} users", adminUID, targetUserUIDs.size());

                Map<String, String> data = Map.of(
                                "type", "ADMIN_CUSTOM",
                                "sentBy", adminUID);

                sendBatchNotifications(targetUserUIDs, title, message, data);
        }

        /**
         * ✅ NEW: Broadcast to all users
         */
        @Async
        @Transactional
        public void broadcastToAllUsers(String title, String message, Map<String, String> data) {
                List<Users> allUsers = usersRepo.findAll();
                List<String> userUIDs = allUsers.stream()
                                .map(Users::getFirebaseUserUID)
                                .collect(Collectors.toList());

                sendBatchNotifications(userUIDs, title, message, data);
        }

        // ==================== CORE FIREBASE MESSAGING ====================

        private void sendToUser(String firebaseUID, String title, String message, Map<String, String> data) {
                try {
                        String deviceToken = getUserDeviceToken(firebaseUID);
                        if (deviceToken == null) {
                                log.warn("No device token for user: {}", firebaseUID);
                                return;
                        }

                        Message fcmMessage = Message.builder()
                                        .setToken(deviceToken)
                                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                                        .setTitle(title)
                                                        .setBody(message)
                                                        .build())
                                        .putAllData(data)
                                        .setAndroidConfig(AndroidConfig.builder()
                                                        .setPriority(AndroidConfig.Priority.HIGH)
                                                        .setNotification(AndroidNotification.builder()
                                                                        .setChannelId("tournament_channel_v2")
                                                                        .setColor("#4CAF50")
                                                                        .setSound("default")
                                                                        .build())
                                                        .build())
                                        .build();

                        FirebaseMessaging.getInstance().send(fcmMessage);
                        log.info("✅ Notification sent to user: {}", firebaseUID);

                } catch (FirebaseMessagingException e) {
                        log.error("Failed to send notification to {}: {}", firebaseUID, e.getMessage());
                        if ("UNREGISTERED".equals(e.getErrorCode())) {
                                removeInvalidDeviceToken(firebaseUID);
                        }
                }
        }

        private void sendBatchNotifications(List<String> firebaseUIDs, String title,
                        String message, Map<String, String> data) {
                List<String> validTokens = firebaseUIDs.stream()
                                .map(this::getUserDeviceToken)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                if (validTokens.isEmpty()) {
                        log.warn("No valid device tokens found");
                        return;
                }

                try {
                        MulticastMessage multicastMessage = MulticastMessage.builder()
                                        .addAllTokens(validTokens)
                                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                                        .setTitle(title)
                                                        .setBody(message)
                                                        .build())
                                        .putAllData(data)
                                        .setAndroidConfig(AndroidConfig.builder()
                                                        .setPriority(AndroidConfig.Priority.HIGH)
                                                        .setNotification(AndroidNotification.builder()
                                                                        .setChannelId("tournament_channel_v2")
                                                                        .setColor("#4CAF50")
                                                                        .setSound("default")
                                                                        .build())
                                                        .build())
                                        .build();

                        BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(multicastMessage);
                        log.info("✅ Batch notification sent: {} successful, {} failed",
                                        response.getSuccessCount(), response.getFailureCount());

                } catch (Exception e) {
                        log.error("Failed to send batch notifications", e);
                }
        }

        private String getUserDeviceToken(String firebaseUID) {
                return usersRepo.findByFirebaseUserUID(firebaseUID)
                                .map(Users::getDeviceToken)
                                .orElse(null);
        }

        private void removeInvalidDeviceToken(String firebaseUID) {
                usersRepo.findByFirebaseUserUID(firebaseUID).ifPresent(user -> {
                        user.setDeviceToken(null);
                        usersRepo.save(user);
                });
        }

        private String formatDateTime(LocalDateTime dateTime) {
                return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        }
}