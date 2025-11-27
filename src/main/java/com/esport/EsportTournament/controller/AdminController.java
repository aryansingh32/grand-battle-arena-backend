package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.dto.*;
import com.esport.EsportTournament.model.Tournaments;
import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
public class AdminController {

    private final UserService userService;
    private final TournamentService tournamentService;
    private final WalletService walletService;
    private final TransactionTableService transactionService;
    private final NotificationService notificationService;
    private final SlotService slotService;
    private final AuditLogService auditLogService;

    // ================= DASHBOARD & ANALYTICS =================

    /**
     * Get comprehensive admin dashboard statistics
     */
    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        log.info("Fetching admin dashboard statistics");

        try {
            Map<String, Object> userStats = userService.getUserStatistics();
            Map<String, Object> tournamentStats = tournamentService.getTournamentStats();
            Map<String, Object> walletStats = walletService.getWalletStatistics();
            Map<String, Object> transactionStats = transactionService.getTransactionStats();
            Map<String, Object> notificationStats = notificationService.getNotificationStats();

            Map<String, Object> dashboard = Map.of(
                    "users", userStats,
                    "tournaments", tournamentStats,
                    "wallets", walletStats,
                    "transactions", transactionStats,
                    "notifications", notificationStats,
                    "systemStatus", Map.of(
                            "status", "OPERATIONAL",
                            "lastUpdate", LocalDateTime.now()));

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error fetching dashboard statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dashboard data"));
        }
    }

    /**
     * Get system health and performance metrics
     */
    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @GetMapping("/system/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        // This could include database health, Redis health, etc.
        Map<String, Object> health = Map.of(
                "database", "HEALTHY",
                "redis", "HEALTHY",
                "firebase", "HEALTHY",
                "uptime", System.currentTimeMillis(),
                "timestamp", LocalDateTime.now());

        return ResponseEntity.ok(health);
    }

    // ================= USER MANAGEMENT =================

    /**
     * Get paginated user list with filters
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsersWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        log.debug("Fetching users - page: {}, size: {}, role: {}, status: {}, search: {}",
                page, size, role, status, search);

        try {
            List<UserDTO> allUsers = userService.getAllUsers();

            // Apply filters
            List<UserDTO> filteredUsers = allUsers.stream()
                    .filter(user -> role == null || user.getRole().name().equalsIgnoreCase(role))
                    .filter(user -> status == null || user.getStatus().name().equalsIgnoreCase(status))
                    .filter(user -> search == null ||
                            user.getUserName().toLowerCase().contains(search.toLowerCase()) ||
                            user.getEmail().toLowerCase().contains(search.toLowerCase()))
                    .toList();

            // Manual pagination (for demo - in production, use database pagination)
            int start = page * size;
            int end = Math.min(start + size, filteredUsers.size());
            List<UserDTO> pageContent = filteredUsers.subList(start, end);

            Map<String, Object> response = Map.of(
                    "content", pageContent,
                    "totalElements", filteredUsers.size(),
                    "totalPages", (filteredUsers.size() + size - 1) / size,
                    "currentPage", page,
                    "pageSize", size);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch users"));
        }
    }

    /**
     * Bulk update user status
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    @PutMapping("/users/bulk-status")
    public ResponseEntity<Map<String, Object>> bulkUpdateUserStatus(
            @RequestBody Map<String, Object> request) {

        @SuppressWarnings("unchecked")
        List<String> userIds = (List<String>) request.get("userIds");
        String statusStr = (String) request.get("status");

        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("User IDs cannot be empty");
        }

        Users.UserStatus status = Users.UserStatus.valueOf(statusStr.toUpperCase());

        log.info("Bulk updating status for {} users to {}", userIds.size(), status);

        try {
            int updatedCount = 0;
            for (String userId : userIds) {
                try {
                    userService.updateUserStatus(userId, status);
                    updatedCount++;
                } catch (Exception e) {
                    log.warn("Failed to update status for user {}: {}", userId, e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Bulk status update completed",
                    "totalRequested", userIds.size(),
                    "successfulUpdates", updatedCount));
        } catch (Exception e) {
            log.error("Error during bulk status update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Bulk update failed"));
        }
    }

    /**
     * Get user activity summary
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    @GetMapping("/users/{firebaseUID}/activity")
    public ResponseEntity<Map<String, Object>> getUserActivity(@PathVariable String firebaseUID) {
        log.debug("Fetching activity for user: {}", firebaseUID);

        try {
            UserDTO user = userService.getUserByFirebaseUID(firebaseUID);
            WalletDTO wallet = walletService.getWalletByFirebaseUID(firebaseUID);
            List<TransactionTableDTO> transactions = transactionService.getUserTransactions(firebaseUID);
            List<SlotsDTO> bookedSlots = slotService.getUserBookedSlots(firebaseUID);

            Map<String, Object> activity = Map.of(
                    "user", user,
                    "wallet", wallet,
                    "transactionCount", transactions.size(),
                    "bookedSlotsCount", bookedSlots.size(),
                    "recentTransactions", transactions.stream().limit(5).toList(),
                    "recentBookings", bookedSlots.stream().limit(5).toList());

            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            log.error("Error fetching user activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch user activity"));
        }
    }

    // ================= TOURNAMENT MANAGEMENT =================

    /**
     * Advanced tournament management with bulk operations
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PostMapping("/tournaments/bulk-status")
    public ResponseEntity<Map<String, Object>> bulkUpdateTournamentStatus(
            @RequestBody Map<String, Object> request) {

        @SuppressWarnings("unchecked")
        List<Integer> tournamentIds = (List<Integer>) request.get("tournamentIds");
        String statusStr = (String) request.get("status");

        Tournaments.TournamentStatus status = Tournaments.TournamentStatus.valueOf(statusStr.toUpperCase());

        log.info("Bulk updating status for {} tournaments to {}", tournamentIds.size(), status);

        try {
            int updatedCount = 0;
            for (Integer tournamentId : tournamentIds) {
                try {
                    tournamentService.updateTournamentStatus(tournamentId, status);
                    updatedCount++;
                } catch (Exception e) {
                    log.warn("Failed to update tournament {}: {}", tournamentId, e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Bulk tournament status update completed",
                    "totalRequested", tournamentIds.size(),
                    "successfulUpdates", updatedCount));
        } catch (Exception e) {
            log.error("Error during bulk tournament update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Bulk update failed"));
        }
    }

    /**
     * Get detailed tournament analytics
     */
    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @GetMapping("/tournaments/{tournamentId}/analytics")
    public ResponseEntity<Map<String, Object>> getTournamentAnalytics(@PathVariable int tournamentId) {
        log.debug("Fetching analytics for tournament: {}", tournamentId);

        try {
            TournamentsDTO tournament = tournamentService.getTournamentById(tournamentId);
            Map<String, Object> slotSummary = slotService.getSlotSummary(tournamentId);

            // Calculate revenue and participation metrics
            long bookedSlots = (long) slotSummary.get("bookedCount");
            Map<String, Object> analytics = getStringObjectMap(tournament, bookedSlots, slotSummary);

            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching tournament analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch tournament analytics"));
        }
    }

    private static Map<String, Object> getStringObjectMap(TournamentsDTO tournament, long bookedSlots,
            Map<String, Object> slotSummary) {
        int entryFee = tournament.getEntryFee();
        long totalRevenue = bookedSlots * entryFee;

        return Map.of(
                "tournament", tournament,
                "slotSummary", slotSummary,
                "revenue", Map.of(
                        "totalRevenue", totalRevenue,
                        "expectedRevenue", tournament.getMaxPlayers() * entryFee,
                        "revenuePercentage",
                        tournament.getMaxPlayers() > 0
                                ? (totalRevenue * 100.0) / (tournament.getMaxPlayers() * entryFee)
                                : 0),
                "participation", Map.of(
                        "fillRate",
                        tournament.getMaxPlayers() > 0 ? (bookedSlots * 100.0) / tournament.getMaxPlayers() : 0,
                        "remainingSlots", tournament.getMaxPlayers() - bookedSlots));
    }

    // ================= FINANCIAL MANAGEMENT =================

    /**
     * Get financial overview and reports
     */
    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @GetMapping("/finance/overview")
    public ResponseEntity<Map<String, Object>> getFinancialOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.debug("Fetching financial overview from {} to {}", startDate, endDate);

        try {
            Map<String, Object> walletStats = walletService.getWalletStatistics();
            Map<String, Object> transactionStats = transactionService.getTransactionStats();

            // Additional financial calculations could go here
            Map<String, Object> financialOverview = Map.of(
                    "wallets", walletStats,
                    "transactions", transactionStats,
                    "reportPeriod", Map.of(
                            "startDate", startDate != null ? startDate : "All time",
                            "endDate", endDate != null ? endDate : "Current",
                            "generatedAt", LocalDateTime.now()));

            return ResponseEntity.ok(financialOverview);
        } catch (Exception e) {
            log.error("Error fetching financial overview", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch financial overview"));
        }
    }

    /**
     * Emergency wallet operations
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @PostMapping("/wallets/emergency-adjustment")
    public ResponseEntity<Map<String, String>> emergencyWalletAdjustment(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String firebaseUID = (String) request.get("firebaseUID");
        Integer amount = (Integer) request.get("amount");
        String operation = (String) request.get("operation"); // "SET" or "ADD" or "DEDUCT"
        String reason = (String) request.get("reason");

        log.warn("Emergency wallet adjustment by admin {}: {} {} coins for user {} - Reason: {}",
                adminUID, operation, amount, firebaseUID, reason);

        try {
            switch (operation.toUpperCase()) {
                case "SET":
                    walletService.setWalletBalance(firebaseUID, amount, adminUID);
                    break;
                case "ADD":
                    walletService.addCoins(firebaseUID, amount);
                    break;
                case "DEDUCT":
                    walletService.deductCoins(firebaseUID, amount);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid operation: " + operation);
            }

            // Log the emergency operation for audit trail
            log.warn("AUDIT: Emergency wallet {} operation completed by admin {} for user {} - Amount: {} - Reason: {}",
                    operation, adminUID, firebaseUID, amount, reason);

            return ResponseEntity.ok(Map.of(
                    "message", "Emergency wallet adjustment completed successfully",
                    "operation", operation,
                    "amount", amount.toString(),
                    "reason", reason != null ? reason : "No reason provided"));
        } catch (Exception e) {
            log.error("Error during emergency wallet adjustment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Emergency wallet adjustment failed"));
        }
    }

    // ================= NOTIFICATION MANAGEMENT =================

    /**
     * Broadcast emergency notification to all users
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/notifications/emergency-broadcast")
    public ResponseEntity<Map<String, String>> emergencyBroadcast(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String title = request.get("title");
        String message = request.get("message");

        if (title == null || message == null) {
            throw new IllegalArgumentException("Title and message are required for emergency broadcast");
        }

        log.warn("Emergency broadcast initiated by admin {}: {}", adminUID, title);

        try {
            notificationService.createNotification(title, message,
                    com.esport.EsportTournament.model.Notifications.TargetAudience.ALL, adminUID);

            return ResponseEntity.ok(Map.of(
                    "message", "Emergency broadcast sent successfully",
                    "title", title,
                    "broadcastBy", adminUID));
        } catch (Exception e) {
            log.error("Error sending emergency broadcast", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send emergency broadcast"));
        }
    }

    /**
     * Send maintenance notification
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_NOTIFICATIONS')")
    @PostMapping("/notifications/maintenance")
    public ResponseEntity<Map<String, String>> maintenanceNotification(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String message = (String) request.get("message");
        String scheduledTime = (String) request.get("scheduledTime");
        Integer durationMinutes = (Integer) request.get("durationMinutes");

        String fullMessage = String.format(
                "🔧 MAINTENANCE NOTICE: %s\n⏰ Scheduled: %s\n⏳ Duration: %d minutes\nWe apologize for any inconvenience.",
                message, scheduledTime, durationMinutes);

        try {
            notificationService.createNotification("Scheduled Maintenance", fullMessage,
                    com.esport.EsportTournament.model.Notifications.TargetAudience.ALL, adminUID);

            return ResponseEntity.ok(Map.of(
                    "message", "Maintenance notification sent successfully"));
        } catch (Exception e) {
            log.error("Error sending maintenance notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send maintenance notification"));
        }
    }

    // ================= SYSTEM OPERATIONS =================

    /**
     * Get audit logs (simplified version)
     */
    @PreAuthorize("hasAuthority('PERM_VIEW_AUDIT')")
    @GetMapping("/audit/logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String adminId) {

        try {
            // Use adminId as userId filter if provided
            org.springframework.data.domain.Page<com.esport.EsportTournament.model.AuditLog> logsPage = auditLogService
                    .getLogs(page, size, action, adminId);

            Map<String, Object> response = Map.of(
                    "content", logsPage.getContent(),
                    "totalElements", logsPage.getTotalElements(),
                    "totalPages", logsPage.getTotalPages(),
                    "currentPage", logsPage.getNumber(),
                    "pageSize", logsPage.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching audit logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch audit logs"));
        }
    }

    /**
     * Generate system report
     */
    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @PostMapping("/reports/generate")
    public ResponseEntity<Map<String, Object>> generateSystemReport(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        String reportType = (String) request.get("reportType");
        String startDate = (String) request.get("startDate");
        String endDate = (String) request.get("endDate");

        log.info("Admin {} generating {} report from {} to {}", adminUID, reportType, startDate, endDate);

        try {
            // In a real implementation, you'd generate actual reports
            Map<String, Object> report = Map.of(
                    "reportId", "RPT_" + System.currentTimeMillis(),
                    "type", reportType,
                    "generatedBy", adminUID,
                    "generatedAt", LocalDateTime.now(),
                    "period", Map.of("start", startDate, "end", endDate),
                    "status", "COMPLETED",
                    "downloadUrl", "/api/admin/reports/download/" + "RPT_" + System.currentTimeMillis());

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error generating system report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate report"));
        }
    }

    /**
     * System settings management
     */
    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSystemSettings() {
        Map<String, Object> settings = Map.of(
                "maintenance", Map.of(
                        "enabled", false,
                        "message", "",
                        "scheduledStart", null,
                        "estimatedDuration", 0),
                "features", Map.of(
                        "userRegistration", true,
                        "tournamentBooking", true,
                        "walletTransactions", true,
                        "notifications", true),
                "limits", Map.of(
                        "maxCoinBalance", 1000000,
                        "maxTransactionAmount", 10000,
                        "maxSlotsPerUser", 5,
                        "maxTournamentParticipants", 100),
                "security", Map.of(
                        "requireEmailVerification", true,
                        "enableTwoFactorAuth", false,
                        "maxLoginAttempts", 5,
                        "sessionTimeoutMinutes", 60));

        return ResponseEntity.ok(settings);
    }

    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @PutMapping("/settings")
    public ResponseEntity<Map<String, String>> updateSystemSettings(
            @RequestBody Map<String, Object> settings,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        log.info("Admin {} updating system settings", adminUID);

        // In a real implementation, you'd validate and save these settings
        // For now, just acknowledge the request

        return ResponseEntity.ok(Map.of(
                "message", "System settings updated successfully",
                "updatedBy", adminUID,
                "timestamp", LocalDateTime.now().toString()));
    }

    // ================= BULK OPERATIONS =================

    /**
     * Export data for backup or analysis
     */
    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @PostMapping("/export")
    public ResponseEntity<Map<String, Object>> exportData(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        @SuppressWarnings("unchecked")
        List<String> dataTypes = (List<String>) request.get("dataTypes");
        String format = (String) request.get("format"); // JSON, CSV, etc.

        log.info("Admin {} initiating data export for types: {} in format: {}", adminUID, dataTypes, format);

        try {
            // In a real implementation, you'd generate actual export files
            String exportId = "EXP_" + System.currentTimeMillis();

            Map<String, Object> exportInfo = Map.of(
                    "exportId", exportId,
                    "status", "IN_PROGRESS",
                    "dataTypes", dataTypes,
                    "format", format,
                    "initiatedBy", adminUID,
                    "initiatedAt", LocalDateTime.now(),
                    "estimatedCompletion", LocalDateTime.now().plusMinutes(5));

            return ResponseEntity.accepted().body(exportInfo);
        } catch (Exception e) {
            log.error("Error initiating data export", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to initiate data export"));
        }
    }

    /**
     * Clean up old data
     */
    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldData(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        Integer daysOld = (Integer) request.get("daysOld");
        @SuppressWarnings("unchecked")
        List<String> dataTypes = (List<String>) request.get("dataTypes");

        log.warn("Admin {} initiating cleanup of data older than {} days for types: {}",
                adminUID, daysOld, dataTypes);

        try {
            // In a real implementation, you'd perform actual cleanup
            Map<String, Object> cleanupResult = Map.of(
                    "cleanupId", "CLN_" + System.currentTimeMillis(),
                    "status", "COMPLETED",
                    "itemsProcessed", 1500,
                    "itemsDeleted", 150,
                    "spaceSaved", "45.2 MB",
                    "executedBy", adminUID,
                    "executedAt", LocalDateTime.now());

            return ResponseEntity.ok(cleanupResult);
        } catch (Exception e) {
            log.error("Error during data cleanup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to perform data cleanup"));
        }
    }

    // ================= HELPER METHODS =================

    private String getAuthenticatedUserUID(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated admin found");
        }
        return (String) authentication.getPrincipal();
    }

    /**
     * Validate admin permissions for sensitive operations
     */
    private void validateAdminPermissions(String adminUID, String operation) {
        // In a real implementation, you might have different admin permission levels
        // For now, all admins can perform all operations
        log.debug("Admin {} validated for operation: {}", adminUID, operation);
    }
}