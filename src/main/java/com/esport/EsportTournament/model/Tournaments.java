package com.esport.EsportTournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Component
@Table(name = "tournaments")
public class Tournaments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String mapType;
    private LocalDateTime startTime;
    private int entryFees;
    private int prizePool;

    // 🔥 CRITICAL FIX: Store as String for better compatibility
    @Column(name = "team_size", length = 20, nullable = false)
    private String teamSize; // Stores "SOLO", "DUO", "SQUAD", "HEXA"

    private int maxPlayers;
    private String game;
    private String imageLink;

    @Enumerated(EnumType.STRING)
    private TournamentStatus status;

    private LocalDateTime updatedAt;
    private String gameId;
    private String gamePassword;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Rules stored as JSON array string
    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    // Prize fields
    @Column(name = "per_kill_reward")
    private Integer perKillReward;

    @Column(name = "first_prize")
    private Integer firstPrize;

    @Column(name = "second_prize")
    private Integer secondPrize;

    @Column(name = "third_prize")
    private Integer thirdPrize;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        // 🔥 NORMALIZE team size on create
        normalizeTeamSize();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalizeTeamSize();
    }

    @PostLoad
    protected void onLoad() {
        // 🔥 CRITICAL: Ensure teamSize is never null when loaded from database
        // This handles cases where old data might have null teamSize
        normalizeTeamSize();
    }

    // 🔥 CRITICAL: Normalize team size to consistent format
    private void normalizeTeamSize() {
        if (teamSize == null || teamSize.trim().isEmpty()) {
            teamSize = "SOLO";
            return;
        }

        String normalized = teamSize.trim().toUpperCase();

        // Map old enum values or numbers to standard format
        switch (normalized) {
            case "1":
                teamSize = "SOLO";
                break;
            case "2":
                teamSize = "DUO";
                break;
            case "4":
                teamSize = "SQUAD";
                break;
            case "6":
                teamSize = "HEXA";
                break;
            case "SOLO":
            case "DUO":
            case "SQUAD":
            case "HEXA":
                teamSize = normalized; // Already correct
                break;
            default:
                System.err.println("⚠️ Unknown teamSize: '" + teamSize + "', defaulting to SOLO");
                teamSize = "SOLO";
        }

        System.out.println("✅ TeamSize normalized to: " + teamSize);
    }

    // 🔥 HELPER: Get players per team
    public int getPlayersPerTeam() {
        if (teamSize == null) return 1;

        switch (teamSize.toUpperCase()) {
            case "SOLO": return 1;
            case "DUO": return 2;
            case "SQUAD": return 4;
            case "HEXA": return 6;
            default: return 1;
        }
    }

    public enum TournamentStatus {
        UPCOMING,
        ONGOING,
        COMPLETED,
        CANCELLED
    }
}