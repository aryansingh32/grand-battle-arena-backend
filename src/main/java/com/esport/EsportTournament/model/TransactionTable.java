package com.esport.EsportTournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
// Withdraw TransactionTable
public class TransactionTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Users userId; // firebase User UId
    @Column(name = "transaction_uid")
    private String transactionUID;    //entered by user
    private int amount; //coins
    private TransactionStatus status;

    private String verifiedBy; // which user verified this
    private TransactionType type;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;

    public enum TransactionStatus{
        PENDING,
        COMPLETED,
        REJECTED
    }
    public enum TransactionType{
        WITHDRAWAL,
        DEPOSIT
    }

}
