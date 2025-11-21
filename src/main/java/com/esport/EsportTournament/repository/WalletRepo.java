package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepo extends JpaRepository<Wallet, Integer> {

    Optional<Wallet> findByUserId_Id(int userId);

    Optional<Wallet> findByUserId_FirebaseUserUID(String firebaseUID);
}

