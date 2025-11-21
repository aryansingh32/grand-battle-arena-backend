package com.esport.EsportTournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ✅ NEW: Audit Log Entity
 * - Immutable audit trail
 * - Indexes for fast queries
 * - Supports compliance requirements
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_category_timestamp", columnList = "category, timestamp"),
        @Index(name = "idx_user_timestamp", columnList = "user_id, timestamp"),
        @Index(name = "idx_action_timestamp", columnList = "action, timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String category; // TRANSACTION, SLOT, TOURNAMENT, WALLET, USER_MANAGEMENT, SECURITY

    @Column(nullable = false, length = 100)
    private String action; // DEPOSIT_REQUESTED, SLOT_BOOKED, etc.

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId; // Firebase UID

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON or structured text

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}