package com.esport.EsportTournament.dto;

import com.esport.EsportTournament.model.TransactionTable;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TransactionTableDTO {

    private int id;

    private String firebaseUserUID;
    private String transactionUID;
    private int amount;
    private TransactionTable.TransactionType type;
    private TransactionTable.TransactionStatus status;
    private LocalDateTime date;

}
