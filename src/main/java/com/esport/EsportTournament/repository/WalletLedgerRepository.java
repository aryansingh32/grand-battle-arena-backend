package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.WalletLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {

    List<WalletLedger> findByUser_FirebaseUserUIDOrderByCreatedAtDesc(String firebaseUID);
}

