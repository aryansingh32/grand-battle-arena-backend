package com.esport.EsportTournament.service;

import com.esport.EsportTournament.dto.NotificationsDTO;
import com.esport.EsportTournament.exception.ResourceNotFoundException;
import com.esport.EsportTournament.model.Notifications;
import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.repository.NotificationRepo;
import com.esport.EsportTournament.repository.UsersRepo;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepo notificationRepo;
    private final UsersRepo usersRepo;

    // ------------------------ Core Notifications ------------------------

    @Transactional
    public NotificationsDTO createNotification(String title, String message,
            Notifications.TargetAudience targetAudience,
            String adminUID) {
        validateAdmin(adminUID);

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification title cannot be null or empty");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification message cannot be null or empty");
        }
        if (message.length() > 500) {
            throw new IllegalArgumentException("Message cannot exceed 500 characters");
        }

        Notifications notification = new Notifications();
        notification.setTitle(title.trim());
        notification.setMessage(message.trim());
        notification.setTargetAudience(targetAudience);
        notification.setCreatedBy(adminUID);

        Notifications savedNotification = notificationRepo.save(notification);

        // Send push notifications based on target audience
        sendPushNotificationByAudience(savedNotification);

        return mapToDTO(savedNotification, adminUID);
    }

    @Transactional
    public NotificationsDTO sendNotificationToUser(String firebaseUID, String title, String message, String adminUID) {
        validateAdmin(adminUID);
        validateUser(firebaseUID);

        // Create notification in database
        Notifications notification = new Notifications();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTargetAudience(Notifications.TargetAudience.USER);
        notification.setCreatedBy(adminUID);

        Notifications savedNotification = notificationRepo.save(notification);

        // Send push notification to specific user
        sendPushNotificationToUser(firebaseUID, title, message, null);

        return mapToDTO(savedNotification, adminUID);
    }

    @Transactional(readOnly = true)
    public List<NotificationsDTO> getNotificationsForUser(String firebaseUID) {
        validateUser(firebaseUID);

        return notificationRepo.findAll().stream()
                .filter(n -> n.getTargetAudience() == Notifications.TargetAudience.ALL ||
                        n.getTargetAudience() == Notifications.TargetAudience.USER)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(n -> mapToDTO(n, firebaseUID))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationsDTO> getNotificationsForAdmin(String adminUID) {
        validateAdmin(adminUID);

        return notificationRepo.findAll().stream()
                .filter(n -> n.getTargetAudience() == Notifications.TargetAudience.ALL ||
                        n.getTargetAudience() == Notifications.TargetAudience.ADMIN)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(n -> mapToDTO(n, adminUID))
                .collect(Collectors.toList());
    }

    @Transactional
    public void markNotificationAsRead(int notificationId, String firebaseUID) {
        // For now, just log it. You can extend this to track read status per user
        log.info("User {} marked notification {} as read", firebaseUID, notificationId);
    }

    @Transactional
    public void deleteNotification(int notificationId, String adminUID) {
        validateAdmin(adminUID);

        if (!notificationRepo.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification not found with ID: " + notificationId);
        }
        notificationRepo.deleteById(notificationId);
    }

    // ------------------------ Tournament Notifications ------------------------

    @Transactional
    public void sendTournamentNotification(int tournamentId, String title, String message,
            List<String> participantUIDs, String notificationType,
            Map<String, String> additionalData) {
        if (participantUIDs == null || participantUIDs.isEmpty()) {
            log.warn("No participants to send notification to for tournament {}", tournamentId);
            return;
        }

        // Save notification to database
        Notifications notification = new Notifications();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTargetAudience(Notifications.TargetAudience.REGISTERED);
        notification.setCreatedBy("SYSTEM"); // System generated
        notificationRepo.save(notification);

        // Prepare notification data
        Map<String, String> data = new HashMap<>();
        data.put("type", notificationType);
        data.put("tournamentId", String.valueOf(tournamentId));
        if (additionalData != null) {
            data.putAll(additionalData);
        }

        // Send push notifications to all participants
        for (String participantUID : participantUIDs) {
            sendPushNotificationToUser(participantUID, title, message, data);
        }

        log.info("Sent {} notification to {} participants for tournament {}",
                notificationType, participantUIDs.size(), tournamentId);
    }

    public void sendGameCredentials(int tournamentId, String gameId, String gamePassword,
            List<String> participantUIDs) {
        String title = "🎮 Tournament Game Credentials";
        // ENHANCED: Show actual credentials in notification message
        String message = String.format(
                "Your Custom ID: %s\nCustom Password: %s\nJoin fast!",
                gameId, gamePassword);

        Map<String, String> data = new HashMap<>();
        data.put("gameId", gameId);
        data.put("gamePassword", gamePassword);
        data.put("tournamentName", "Tournament #" + tournamentId);
        data.put("action_copy_id", gameId); // For copy action
        data.put("action_type", "copy_game_id"); // Action identifier

        sendTournamentNotification(tournamentId, title, message, participantUIDs,
                "tournament_credentials", data);
    }

    public void sendTournamentReminder(int tournamentId, String tournamentName,
            List<String> participantUIDs, int minutesBefore) {
        String title = "Tournament Reminder";
        String message = String.format("Tournament '%s' starts in %d minutes! Get ready!",
                tournamentName, minutesBefore);

        Map<String, String> data = new HashMap<>();
        data.put("tournamentName", tournamentName);
        data.put("minutesBefore", String.valueOf(minutesBefore));

        sendTournamentNotification(tournamentId, title, message, participantUIDs,
                "tournament_reminder", data);
    }

    public void sendTournamentResult(int tournamentId, String resultMessage,
            List<String> participantUIDs) {
        sendTournamentNotification(tournamentId, "Tournament Results", resultMessage,
                participantUIDs, "tournament_result", null);
    }

    public void notifyLiveParticipantsUpdate(int tournamentId, List<String> participantUIDs) {
        sendTournamentNotification(tournamentId,
                "Live Participants Update",
                "Participant list has been updated for Tournament #" + tournamentId,
                participantUIDs,
                "tournament_update",
                null);
    }

    // ------------------------ Wallet & Rewards ------------------------

    public void notifyWalletTransaction(String firebaseUID, String message, String adminUID) {
        validateUser(firebaseUID);

        // Create notification
        createNotification("Wallet Update", message, Notifications.TargetAudience.USER, adminUID);

        // Send push notification
        Map<String, String> data = new HashMap<>();
        data.put("type", "wallet_transaction");
        sendPushNotificationToUser(firebaseUID, "Wallet Update", message, data);
    }

    public void notifyRewardDistribution(String firebaseUID, String rewardMessage, String adminUID) {
        validateUser(firebaseUID);

        createNotification("Reward Distribution", rewardMessage, Notifications.TargetAudience.USER, adminUID);

        Map<String, String> data = new HashMap<>();
        data.put("type", "reward_distribution");
        sendPushNotificationToUser(firebaseUID, "Reward Distribution", rewardMessage, data);
    }

    // ------------------------ Enhanced Notifications ------------------------

    /**
     * Notify all users when a new tournament is created
     */
    public void sendTournamentCreatedNotification(int tournamentId, String tournamentName,
            int prizePool, int entryFee, String startTime) {
        String title = "🏆 New Tournament Created!";
        String message = String.format(
                "Tournament: %s\nPrize Pool: ₹%,d\nEntry Fee: ₹%,d\nStarts: %s",
                tournamentName, prizePool, entryFee, startTime);

        // Get all active users
        List<String> allUserTokens = getAllUserTokens();

        Map<String, String> data = new HashMap<>();
        data.put("tournamentId", String.valueOf(tournamentId));
        data.put("tournamentName", tournamentName);

        sendBroadcastNotification(title, message, allUserTokens, "tournament_created", data);
    }

    /**
     * Remind all users to book tournament slots
     */
    public void sendBookingReminderNotification(int tournamentId, String tournamentName, int remainingSlots) {
        String title = "⚡ Slots Filling Fast!";
        String message = String.format(
                "Tournament: %s\nOnly %d slots remaining!\nBook now before it's full!",
                tournamentName, remainingSlots);

        List<String> allUserTokens = getAllUserTokens();

        Map<String, String> data = new HashMap<>();
        data.put("tournamentId", String.valueOf(tournamentId));
        data.put("tournamentName", tournamentName);
        data.put("remainingSlots", String.valueOf(remainingSlots));

        sendBroadcastNotification(title, message, allUserTokens, "tournament_booking_reminder", data);
    }

    /**
     * Send custom notification to all users or specific users
     */
    public void sendCustomNotification(String title, String message, List<String> targetUserUIDs, String adminUID) {
        validateAdmin(adminUID);

        // Save to database
        Notifications notification = new Notifications();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTargetAudience(
                targetUserUIDs == null || targetUserUIDs.isEmpty() 
                    ? Notifications.TargetAudience.ALL 
                    : Notifications.TargetAudience.USER);
        notification.setCreatedBy(adminUID);
        notificationRepo.save(notification);

        Map<String, String> data = new HashMap<>();
        data.put("type", "custom_notification");

        if (targetUserUIDs == null || targetUserUIDs.isEmpty()) {
            // Send to all users
            List<String> allUserTokens = getAllUserTokens();
            sendBroadcastNotification(title, message, allUserTokens, "custom_notification", data);
        } else {
            // Send to specific users
            for (String userUID : targetUserUIDs) {
                sendPushNotificationToUser(userUID, title, message, data);
            }
        }

        log.info("Custom notification sent: '{}' to {} users", title,
                targetUserUIDs == null ? "all" : targetUserUIDs.size());
    }

    /**
     * Send event notification to all users
     */
    public void sendEventNotification(String eventTitle, String  eventMessage, String eventDate, String adminUID) {
        validateAdmin(adminUID);

        String title = "🎉 " + eventTitle;
        String message = String.format("%s\nEvent Date: %s", eventMessage, eventDate);

        // Save to database
        Notifications notification = new Notifications();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setTargetAudience(Notifications.TargetAudience.ALL);
        notification.setCreatedBy(adminUID);
        notificationRepo.save(notification);

        List<String> allUserTokens = getAllUserTokens();

        Map<String, String> data = new HashMap<>();
        data.put("type", "event_notification");
        data.put("eventDate", eventDate);

        sendBroadcastNotification(title, message, allUserTokens, "event_notification", data);
    }

    /**
     * Send tournament rules to participants
     */
    public void sendTournamentRulesNotification(int tournamentId, List<String> rules,
            List<String> participantUIDs) {
        String title = "📋 Tournament Rules";
        
        // Format rules as numbered list
        StringBuilder rulesText = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            rulesText.append(String.format("%d. %s\n", i + 1, rules.get(i)));
        }

        Map<String, String> data = new HashMap<>();
        data.put("tournamentId", String.valueOf(tournamentId));

        sendTournamentNotification(tournamentId, title, rulesText.toString().trim(),
                participantUIDs, "tournament_rules", data);
    }

    /**
     * Helper method to send broadcast notifications
     */
    private void sendBroadcastNotification(String title, String message, List<String> userUIDs,
            String type, Map<String, String> additionalData) {
        if (userUIDs == null || userUIDs.isEmpty()) {
            log.warn("No users to send broadcast notification");
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", type);
        if (additionalData != null) {
            data.putAll(additionalData);
        }

        // Send to all users
        for (String userUID : userUIDs) {
            sendPushNotificationToUser(userUID, title, message, data);
        }

        log.info("Broadcast notification '{}' sent to {} users", title, userUIDs.size());
    }

    /**
     * Get all active user tokens
     */
    private List<String> getAllUserTokens() {
        return usersRepo.findAll().stream()
                .map(com.esport.EsportTournament.model.Users::getFirebaseUserUID)
                .filter(uid -> uid != null && !uid.trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    // ------------------------ Firebase Messaging Implementation
    // ------------------------
    // ------------------------ Firebase Messaging Implementation
    // ------------------------

    private void sendPushNotificationByAudience(Notifications notification) {
        try {
            log.info("📢 Sending push notification to audience: {} - Title: {}",
                    notification.getTargetAudience(), notification.getTitle());

            List<String> targetTokens = getDeviceTokensByAudience(notification.getTargetAudience());

            if (targetTokens.isEmpty()) {
                log.warn("⚠️ No device tokens found for audience: {}", notification.getTargetAudience());
                return;
            }

            log.info("📱 Found {} device tokens for audience: {}", targetTokens.size(),
                    notification.getTargetAudience());

            // Prepare notification details
            String title = notification.getTitle();
            String body = notification.getMessage();

            // Base data
            Map<String, String> data = new HashMap<>();
            data.put("type", "general");
            data.put("notificationId", String.valueOf(notification.getId()));

            // Send to multiple tokens
            sendBatchNotifications(targetTokens, title, body, data);

            log.info("✅ Push notification sent successfully to {} devices", targetTokens.size());

        } catch (Exception e) {
            log.error("❌ Error sending push notification for audience {}: {}",
                    notification.getTargetAudience(), e.getMessage(), e);
        }
    }

    private void sendBatchNotifications(List<String> tokens,
            String title,
            String body,
            Map<String, String> data) {
        if (tokens.isEmpty())
            return;

        try {
            // Build multicast message with proper Android/iOS config
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                    .putAllData(data != null ? data : Collections.emptyMap())
                    .setAndroidConfig(
                            AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .setNotification(
                                            AndroidNotification.builder()
                                                    .setColor("#4CAF50")
                                                    .setSound("default")
                                                    .setChannelId("tournament_channel_v2")
                                                    .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                                    .build())
                                    .build())
                    .setApnsConfig(
                            ApnsConfig.builder()
                                    .setAps(
                                            Aps.builder()
                                                    .setAlert(
                                                            ApsAlert.builder()
                                                                    .setTitle(title)
                                                                    .setBody(body)
                                                                    .build())
                                                    .setSound("default")
                                                    .setBadge(1)
                                                    .setContentAvailable(true)
                                                    .build())
                                    .build())
                    .build();

            // Verify Firebase is initialized before sending
            if (FirebaseApp.getApps().isEmpty()) {
                log.error("❌ CRITICAL: Firebase is not initialized! Cannot send batch notifications.");
                throw new IllegalStateException("Firebase Admin SDK is not initialized");
            }

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            log.info("📢 Batch notification sent: {} successful, {} failed",
                    response.getSuccessCount(), response.getFailureCount());

            // Handle failed tokens
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        String failedToken = tokens.get(i);
                        FirebaseMessagingException exception = responses.get(i).getException();

                        log.warn("⚠️ Failed to send to token [{}]: {}",
                                failedToken, exception != null ? exception.getMessage() : "Unknown error");

                        // Remove invalid tokens
                        if (exception != null && ("UNREGISTERED".equals(exception.getErrorCode())
                                || "INVALID_ARGUMENT".equals(exception.getErrorCode()))) {
                            removeInvalidDeviceTokenByToken(failedToken);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("❌ Error sending batch notifications: {}", e.getMessage());
        }
    }

    private void sendPushNotificationToUser(String firebaseUID,
            String title,
            String message,
            Map<String, String> additionalData) {
        try {
            // Get user's device token
            String deviceToken = getUserDeviceToken(firebaseUID);
            if (deviceToken == null || deviceToken.trim().isEmpty()) {
                log.warn("No device token found for user: {}", firebaseUID);
                return;
            }

            // Build notification with proper configuration for both Android and iOS
            Message.Builder builder = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(message)
                                    .build())
                    .setAndroidConfig(
                            AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .setNotification(
                                            AndroidNotification.builder()
                                                    .setColor("#4CAF50")
                                                    .setSound("default")
                                                    .setChannelId("tournament_channel_v2")
                                                    .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                                    .build())
                                    .build())
                    .setApnsConfig(
                            ApnsConfig.builder()
                                    .setAps(
                                            Aps.builder()
                                                    .setAlert(
                                                            ApsAlert.builder()
                                                                    .setTitle(title)
                                                                    .setBody(message)
                                                                    .build())
                                                    .setSound("default")
                                                    .setBadge(1)
                                                    .setContentAvailable(true)
                                                    .build())
                                    .build());

            // Add additional data (if any)
            if (additionalData != null && !additionalData.isEmpty()) {
                builder.putAllData(additionalData);
            }

            // Build final message
            Message firebaseMessage = builder.build();

            // Verify Firebase is initialized before sending
            if (FirebaseApp.getApps().isEmpty()) {
                log.error("❌ CRITICAL: Firebase is not initialized! Cannot send notification.");
                throw new IllegalStateException("Firebase Admin SDK is not initialized");
            }

            // Send message
            String response = FirebaseMessaging.getInstance().send(firebaseMessage);
            log.info("✅ Successfully sent push notification to [{}]: Message ID: {}", firebaseUID, response);

        } catch (FirebaseMessagingException e) {
            log.error("❌ Error sending push notification to user [{}]: {} - {}",
                    firebaseUID, e.getErrorCode(), e.getMessage());

            // Handle invalid tokens
            if ("UNREGISTERED".equals(e.getErrorCode()) || "INVALID_ARGUMENT".equals(e.getErrorCode())) {
                removeInvalidDeviceToken(firebaseUID);
            }
        } catch (Exception e) {
            log.error("⚠️ Unexpected error sending push notification to user [{}]: {}", firebaseUID, e.getMessage());
        }
    }

    // private void sendBatchNotifications(List<String> tokens,
    // String title,
    // String body,
    // Map<String, String> data) {
    // if (tokens.isEmpty()) return;
    //
    // try {
    // MulticastMessage message = MulticastMessage.builder()
    // .addAllTokens(tokens)
    // .setNotification(com.google.firebase.messaging.Notification.builder()
    // .setTitle(title)
    // .setBody(body)
    // .build())
    // .putAllData(data != null ? data : Collections.emptyMap())
    // .build();
    //
    // BatchResponse response =
    // FirebaseMessaging.getInstance().sendMulticast(message);
    //
    // log.info("Batch notification sent: {} successful, {} failed",
    // response.getSuccessCount(), response.getFailureCount());
    //
    // } catch (Exception e) {
    // log.error("Error sending batch notifications: {}", e.getMessage());
    // }
    // }

    // ------------------------ Device Token Management ------------------------

    @Transactional
    public void updateUserDeviceToken(String firebaseUID, String deviceToken) {
        try {
            if (deviceToken == null || deviceToken.trim().isEmpty()) {
                log.warn("⚠️ Empty device token provided for user: {}", firebaseUID);
                return;
            }

            Users user = usersRepo.findByFirebaseUserUID(firebaseUID)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Update device token and timestamp
            user.setDeviceToken(deviceToken.trim());
            user.setDeviceTokenUpdatedAt(LocalDateTime.now());
            usersRepo.save(user);

            log.info("✅ Updated device token for user: {} (token length: {})", firebaseUID, deviceToken.length());

            // Send a test notification to verify token works
            try {
                sendPushNotificationToUser(firebaseUID, "Device Registered",
                        "Your device has been successfully registered for push notifications!",
                        Map.of("type", "device_registered"));
                log.info("✅ Test notification sent to verify device token");
            } catch (Exception e) {
                log.warn("⚠️ Could not send test notification: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("❌ Error updating device token for user {}: {}", firebaseUID, e.getMessage(), e);
            throw new RuntimeException("Failed to update device token: " + e.getMessage(), e);
        }
    }

    private String getUserDeviceToken(String firebaseUID) {
        try {
            Users user = usersRepo.findByFirebaseUserUID(firebaseUID).orElse(null);
            return user != null ? user.getDeviceToken() : null;
        } catch (Exception e) {
            log.error("Error getting device token for user {}: {}", firebaseUID, e.getMessage());
            return null;
        }
    }

    private List<String> getDeviceTokensByAudience(Notifications.TargetAudience audience) {
        try {
            List<Users> users;
            switch (audience) {
                case ALL:
                    users = usersRepo.findAll();
                    break;
                case ADMIN:
                    users = usersRepo.findByRole(Users.UserRole.ADMIN);
                    break;
                case USER:
                    users = usersRepo.findByRole(Users.UserRole.USER);
                    break;
                default:
                    return Collections.emptyList();
            }

            return users.stream()
                    .map(Users::getDeviceToken)
                    .filter(token -> token != null && !token.trim().isEmpty())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting device tokens for audience {}: {}", audience, e.getMessage());
            return Collections.emptyList();
        }
    }

    private void removeInvalidDeviceToken(String firebaseUID) {
        try {
            Users user = usersRepo.findByFirebaseUserUID(firebaseUID).orElse(null);
            if (user != null) {
                user.setDeviceToken(null);
                usersRepo.save(user);
                log.info("Removed invalid device token for user: {}", firebaseUID);
            }
        } catch (Exception e) {
            log.error("Error removing invalid device token for user {}: {}", firebaseUID, e.getMessage());
        }
    }

    private void removeInvalidDeviceTokenByToken(String token) {
        try {
            Users user = usersRepo.findByDeviceToken(token).orElse(null);
            if (user != null) {
                user.setDeviceToken(null);
                usersRepo.save(user);
                log.info("Removed invalid device token: {}", token.substring(0, 10) + "...");
            }
        } catch (Exception e) {
            log.error("Error removing invalid device token: {}", e.getMessage());
        }
    }

    // ------------------------ Statistics ------------------------

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStats() {
        List<Notifications> all = notificationRepo.findAll();
        return Map.of(
                "total", all.size(),
                "forAllUsers",
                all.stream().filter(n -> n.getTargetAudience() == Notifications.TargetAudience.ALL).count(),
                "forUsers",
                all.stream().filter(n -> n.getTargetAudience() == Notifications.TargetAudience.USER).count(),
                "forAdmins",
                all.stream().filter(n -> n.getTargetAudience() == Notifications.TargetAudience.ADMIN).count(),
                "forRegistered",
                all.stream().filter(n -> n.getTargetAudience() == Notifications.TargetAudience.REGISTERED).count());
    }

    // ------------------------ Helpers ------------------------

    private void validateAdmin(String adminUID) {
        Users admin = usersRepo.findByFirebaseUserUID(adminUID)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (admin.getRole() != Users.UserRole.ADMIN) {
            throw new IllegalStateException("Only admins can perform this action");
        }
    }

    private void validateUser(String firebaseUID) {
        usersRepo.findByFirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private NotificationsDTO mapToDTO(Notifications n, String firebaseUserUID) {
        return new NotificationsDTO(n.getId(), firebaseUserUID, n.getMessage(),
                n.getCreatedAt() != null ? n.getCreatedAt() : LocalDateTime.now());
    }
}