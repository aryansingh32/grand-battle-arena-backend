package com.esport.EsportTournament.dto;

import com.esport.EsportTournament.model.WalletLedger;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class WalletLedgerDTO {
    private Long id;
    private String direction;
    private int amount;
    private int balanceAfter;
    private String referenceType;
    private String referenceId;
    private String metadata;
    private String createdBy;
    private LocalDateTime createdAt;

    public static WalletLedgerDTO fromEntity(WalletLedger entry) {
        return new WalletLedgerDTO(
                entry.getId(),
                entry.getDirection().name(),
                entry.getAmount(),
                entry.getBalanceAfter(),
                entry.getReferenceType(),
                entry.getReferenceId(),
                entry.getMetadata(),
                entry.getCreatedBy(),
                entry.getCreatedAt()
        );
    }
}

