package com.esport.EsportTournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tournament_results")
public class TournamentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournaments tournament;

    @Column(name = "firebase_user_uid", nullable = false)
    private String firebaseUserUID;

    @Column(name = "player_name")
    private String playerName;

    @Column(name = "team_name")
    private String teamName;

    private int kills;
    private int placement;

    @Column(name = "coins_earned")
    private int coinsEarned;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
