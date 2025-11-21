package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, Long> {

    // Find active payment by amount
    Optional<Payment> findByAmountAndIsActiveTrue(Integer amount);

    // Find all active payments
    List<Payment> findByIsActiveTrueOrderByAmountAsc();

    // Find all payments (including inactive) for admin
    List<Payment> findAllByOrderByAmountAsc();

    // Check if amount already exists (for validation)
    boolean existsByAmount(Integer amount);

    // Check if amount exists for different payment id (for updates)
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.amount = :amount AND p.id != :id")
    boolean existsByAmountAndNotId(@Param("amount") Integer amount, @Param("id") Long id);

    // Find payment by amount (including inactive) for admin operations
    Optional<Payment> findByAmount(Integer amount);
}