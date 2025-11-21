package com.esport.EsportTournament.service;

import com.esport.EsportTournament.model.AuditLog;
import com.esport.EsportTournament.repository.AuditLogRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ✅ NEW: Comprehensive Audit Logging Service
 * - Tracks all critical operations
 * - Immutable audit trail
 * - Async logging for performance
 * - Compliance ready
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepo auditLogRepo;

    /**
     * Log transaction events
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTransaction(String action, String userId, long amount, String transactionId) {
        Map<String, String> details = new HashMap<>();
        details.put("amount", String.valueOf(amount));
        details.put("transactionId", transactionId);

        createLog("TRANSACTION", action, userId, details);
    }

    /**
     * Log slot booking events
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSlotBooking(String userId, int tournamentId, int slotNumber, int fee) {
        Map<String, String> details = new HashMap<>();
        details.put("tournamentId", String.valueOf(tournamentId));
        details.put("slotNumber", String.valueOf(slotNumber));
        details.put("entryFee", String.valueOf(fee));

        createLog("SLOT", "SLOT_BOOKED", userId, details);
    }

    /**
     * Log team booking events
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTeamBooking(String userId, int tournamentId, int playerCount, int totalCost) {
        Map<String, String> details = new HashMap<>();
        details.put("tournamentId", String.valueOf(tournamentId));
        details.put("playerCount", String.valueOf(playerCount));
        details.put("totalCost", String.valueOf(totalCost));

        createLog("SLOT", "TEAM_BOOKED", userId, details);
    }

    /**
     * Log slot cancellation
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSlotCancellation(String userId, int tournamentId, int slotId, int refund) {
        Map<String, String> details = new HashMap<>();
        details.put("tournamentId", String.valueOf(tournamentId));
        details.put("slotId", String.valueOf(slotId));
        details.put("refundAmount", String.valueOf(refund));

        createLog("SLOT", "SLOT_CANCELLED", userId, details);
    }

    /**
     * Log admin slot cancellation
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAdminSlotCancellation(String adminId, int tournamentId, int slotId, int refund) {
        Map<String, String> details = new HashMap<>();
        details.put("tournamentId", String.valueOf(tournamentId));
        details.put("slotId", String.valueOf(slotId));
        details.put("refundAmount", String.valueOf(refund));

        createLog("ADMIN", "ADMIN_SLOT_CANCELLED", adminId, details);
    }

    /**
     * Log tournament operations
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTournamentOperation(String action, String userId, int tournamentId, Map<String, String> additionalDetails) {
        Map<String, String> details = new HashMap<>(additionalDetails);
        details.put("tournamentId", String.valueOf(tournamentId));

        createLog("TOURNAMENT", action, userId, details);
    }

    /**
     * Log wallet operations
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logWalletOperation(String action, String userId, int amount, int newBalance) {
        Map<String, String> details = new HashMap<>();
        details.put("amount", String.valueOf(amount));
        details.put("newBalance", String.valueOf(newBalance));

        createLog("WALLET", action, userId, details);
    }

    /**
     * Log user management operations
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserManagement(String action, String adminId, String targetUserId, Map<String, String> details) {
        Map<String, String> fullDetails = new HashMap<>(details);
        fullDetails.put("targetUser", targetUserId);

        createLog("USER_MANAGEMENT", action, adminId, fullDetails);
    }

    /**
     * Log security events
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEvent(String event, String userId, String ipAddress, Map<String, String> details) {
        Map<String, String> fullDetails = new HashMap<>(details);
        fullDetails.put("ipAddress", ipAddress);

        createLog("SECURITY", event, userId, fullDetails);
    }

    /**
     * Core logging method
     */
    private void createLog(String category, String action, String userId, Map<String, String> details) {
        try {
            AuditLog log = new AuditLog();
            log.setCategory(category);
            log.setAction(action);
            log.setUserId(userId);
            log.setDetails(details.toString());
            log.setTimestamp(LocalDateTime.now());
            log.setIpAddress(getCurrentIpAddress());

            auditLogRepo.save(log);
//
//            log.info("📝 Audit log created: {} - {} by {}", category, action, userId);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
            // Don't throw exception - logging should not break main flow
        }
    }

    /**
     * Get audit logs by category
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByCategory(String category, int limit) {
        return auditLogRepo.findTop100ByCategoryOrderByTimestampDesc(category)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * Get audit logs by user
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUser(String userId, int limit) {
        return auditLogRepo.findTop100ByUserIdOrderByTimestampDesc(userId)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * Get recent audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepo.findTop100ByOrderByTimestampDesc()
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * Get current IP address (placeholder - implement with HttpServletRequest)
     */
    private String getCurrentIpAddress() {
        // In real implementation, inject HttpServletRequest and extract IP
        return "127.0.0.1";
    }
}