package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface WalletRepo extends JpaRepository<Wallet, Integer> {

    Optional<Wallet> findByUserId_Id(int userId);

    Optional<Wallet> findByUserId_FirebaseUserUID(String firebaseUID);

    /**
     * Pessimistic write lock on wallet row.
     * CRITICAL: Prevents concurrent bookings from reading stale balance
     * and overcharging the user. Must be used for ALL coin deductions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId.firebaseUserUID = :firebaseUID")
    Optional<Wallet> findByUserIdForUpdate(@Param("firebaseUID") String firebaseUID);

    @Query("SELECT COALESCE(SUM(w.coins), 0) FROM Wallet w")
    long sumAllCoins();
}
