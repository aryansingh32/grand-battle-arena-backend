package com.esport.EsportTournament.dto;
import lombok.Data;

import java.util.List;


@Data
public class TeamBookingRequestDTO {
    private int tournamentId;
    private List<PlayerInfo> players;

    @Data
    public static class PlayerInfo {
        private int slotNumber;
        private String playerName;
    }
}
