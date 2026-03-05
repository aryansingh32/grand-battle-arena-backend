package com.esport.EsportTournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_reads",
        uniqueConstraints = @UniqueConstraint(name = "uq_notification_read_user", columnNames = { "notification_id", "firebase_useruid" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notifications notification;

    @Column(name = "firebase_useruid", nullable = false, length = 128)
    private String firebaseUserUID;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }
}
