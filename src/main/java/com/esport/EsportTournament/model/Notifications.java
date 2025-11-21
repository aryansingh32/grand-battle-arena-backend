package com.esport.EsportTournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data  // ADDED: Lombok @Data annotation for getters, setters, toString, equals, hashCode
public class Notifications {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String title;

    @Column(length = 500)
    private String message; // FIXED: Changed from 'Message' to 'message' (consistent naming)

    @Enumerated(EnumType.STRING)
    private TargetAudience targetAudience;

    // ADDED: Timestamp fields for better tracking
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ADDED: Who created the notification (admin UID)
    @Column(name = "created_by")
    private String createdBy;

    // ADDED: Pre-persist method to set created timestamp
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    @Column(name = "device_token", length = 500)
    private String deviceToken;

    @Column(name = "device_token_updated_at")
    private LocalDateTime deviceTokenUpdatedAt;

    // ADDED: Pre-update method to update timestamp
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TargetAudience {
        ALL,
        ADMIN,
        USER,
        REGISTERED
    }

    
}