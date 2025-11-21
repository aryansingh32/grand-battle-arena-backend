package com.esport.EsportTournament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TournamentFilterDTO {

    private String gameType;
    private LocalDateTime startTime;

}
