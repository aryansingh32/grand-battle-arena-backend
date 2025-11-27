package com.esport.EsportTournament.dto;

import com.esport.EsportTournament.model.Tournaments;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentsDTO {
    private int id;

    private String name;

    // Getter for "title" - frontend expects "title" field
    @JsonProperty("title")
    public String getTitle() {
        return name;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.name = title;
    }

    private int prizePool;
    private int entryFee;
    private String imageLink;
    private String map;
    private String game;
    private int maxPlayers;
    private LocalDateTime startTime;

    // 🔥 CRITICAL FIX: Return as String, not enum
    private String teamSize; // Will be "SOLO", "DUO", "SQUAD", "HEXA"

    // 🔥 CRITICAL: Getter that ensures teamSize is never null
    @JsonProperty("teamSize")
    public String getTeamSize() {
        if (teamSize == null || teamSize.trim().isEmpty()) {
            return "SOLO"; // Default to SOLO if null
        }
        return teamSize;
    }

    @JsonProperty("teamSize")
    public void setTeamSize(String teamSize) {
        if (teamSize == null || teamSize.trim().isEmpty()) {
            this.teamSize = "SOLO"; // Default to SOLO if null
        } else {
            this.teamSize = teamSize.trim().toUpperCase();
        }
    }

    private Tournaments.TournamentStatus status;
    private String gameId;
    private String gamePassword;

    // 🔥 NEW: Add rules support
    private List<String> rules = new ArrayList<>();

    // 🔥 NEW: Add registered players count
    private int registeredPlayers = 0;

    // 🔥 NEW: Participants list (from slots)
    @JsonProperty("participants")
    private List<ParticipantInfo> participants = new ArrayList<>();

    // 🔥 NEW: Scoreboard data
    @JsonProperty("scoreboard")
    private List<ScoreboardEntry> scoreboard = new ArrayList<>();

    // 🔥 NEW: Prize detail fields
    @JsonProperty("perKillReward")
    private Integer perKillReward;

    @JsonProperty("firstPrize")
    private Integer firstPrize;

    @JsonProperty("secondPrize")
    private Integer secondPrize;

    @JsonProperty("thirdPrize")
    private Integer thirdPrize;

    @JsonProperty("streamUrl")
    private String streamUrl;

    // Inner class for participant info
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo {
        @JsonProperty("playerName")
        private String playerName;

        @JsonProperty("slotNumber")
        private int slotNumber;

        @JsonProperty("userId")
        private String userId;
    }

    // Inner class for scoreboard entry
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreboardEntry {
        @JsonProperty("playerName")
        private String playerName;

        @JsonProperty("teamName")
        private String teamName;

        @JsonProperty("kills")
        private Integer kills;

        @JsonProperty("coinsEarned")
        private Integer coinsEarned;

        @JsonProperty("placement")
        private Integer placement;
    }

    // Constructor without rules (for backward compatibility)
    public TournamentsDTO(int id, String name, int prizePool, int entryFee,
            String imageLink, String map, String game, int maxPlayers,
            LocalDateTime startTime, String teamSize,
            Tournaments.TournamentStatus status, String gameId, String gamePassword) {
        this.id = id;
        this.name = name;
        this.prizePool = prizePool;
        this.entryFee = entryFee;
        this.imageLink = imageLink;
        this.map = map;
        this.game = game;
        this.maxPlayers = maxPlayers;
        this.startTime = startTime;
        this.teamSize = teamSize; // Direct string assignment
        this.status = status;
        this.gameId = gameId;
        this.gamePassword = gamePassword;
    }
}
