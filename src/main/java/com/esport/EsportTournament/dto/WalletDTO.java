package com.esport.EsportTournament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class WalletDTO {
    private int id;
    private String firebaseUserUID;
    private int coins;
    private LocalDateTime lastUpdated;
}
