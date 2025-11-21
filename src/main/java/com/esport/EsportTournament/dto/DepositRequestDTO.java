package com.esport.EsportTournament.dto;

import lombok.Data;

@Data
public class DepositRequestDTO {
    private String transactionUID;
    private int amount;
}
