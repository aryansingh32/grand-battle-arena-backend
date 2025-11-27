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
    private String transactionUID; // entered by user
    private int amount; // coins
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "verified_by")
    private String verifiedBy; // which user verified this
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        REJECTED
    }

    public enum TransactionType {
        WITHDRAWAL,
        DEPOSIT
    }

}
