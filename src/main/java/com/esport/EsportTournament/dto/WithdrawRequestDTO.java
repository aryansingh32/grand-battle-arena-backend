package com.esport.EsportTournament.dto;

import lombok.Data;

@Data
public class WithdrawRequestDTO {
    private Integer amount;
    private String transactionUID; // Used for UPI ID
}
