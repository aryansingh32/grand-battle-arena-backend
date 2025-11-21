package com.esport.EsportTournament.service;

import com.esport.EsportTournament.dto.WalletLedgerDTO;
import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.model.Wallet;
import com.esport.EsportTournament.model.WalletLedger;
import com.esport.EsportTournament.repository.UsersRepo;
import com.esport.EsportTournament.repository.WalletLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletLedgerService {

    private final WalletLedgerRepository walletLedgerRepository;
    private final UsersRepo usersRepo;

    @Transactional
    public void recordEntry(Wallet wallet,
                            WalletLedger.Direction direction,
                            int amount,
                            int balanceAfter,
                            String referenceType,
                            String referenceId,
                            String metadata,
                            String actorUid) {
        WalletLedger ledger = WalletLedger.builder()
                .user(wallet.getUserId())
                .wallet(wallet)
                .direction(direction)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .metadata(metadata)
                .createdBy(actorUid)
                .build();
        walletLedgerRepository.save(ledger);
        log.debug("Ledger entry recorded for user {} direction {} amount {}", wallet.getUserId().getFirebaseUserUID(), direction, amount);
    }

    @Transactional(readOnly = true)
    public List<WalletLedgerDTO> getLedgerForUser(String firebaseUID) {
        List<WalletLedger> entries = walletLedgerRepository.findByUser_FirebaseUserUIDOrderByCreatedAtDesc(firebaseUID);
        return entries.stream().map(WalletLedgerDTO::fromEntity).toList();
    }

    @Transactional
    public void ensureWalletLedgerForUser(String firebaseUID) {
        usersRepo.findByFirebaseUserUID(firebaseUID).orElseThrow(() ->
                new IllegalArgumentException("User not found: " + firebaseUID));
    }
}

