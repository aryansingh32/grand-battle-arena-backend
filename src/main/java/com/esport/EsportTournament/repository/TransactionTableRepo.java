
package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.TransactionTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionTableRepo extends JpaRepository<TransactionTable, Integer> {

    // ========== OPTIMIZED QUERIES ==========

    /**
     * Find transaction by UID with caching
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<TransactionTable> findByTransactionUID(String transactionUID);

    /**
     * Find user transactions with fetch join
     */
    @Query("SELECT DISTINCT t FROM TransactionTable t " +
            "LEFT JOIN FETCH t.userId u " +
            "WHERE u.firebaseUserUID = :firebaseUID " +
            "ORDER BY t.createdAt DESC")
    List<TransactionTable> findByUserId_FirebaseUserUID(@Param("firebaseUID") String firebaseUID);

    /**
     * Check if transaction UID exists (uses unique index)
     */
    boolean existsByTransactionUID(String transactionUID);

    /**
     * Count transactions by status (uses covering index)
     */
    long countByStatus(TransactionTable.TransactionStatus status);

    /**
     * Find user transactions ordered by date
     */
    @Query("SELECT t FROM TransactionTable t " +
            "LEFT JOIN FETCH t.userId u " +
            "WHERE u.firebaseUserUID = :firebaseUID " +
            "ORDER BY t.createdAt DESC")
    List<TransactionTable> findByUserId_FirebaseUserUIDOrderByCreatedAtDesc(
            @Param("firebaseUID") String firebaseUID);

    /**
     * Find transactions by status ordered by creation date
     */
    @Query("SELECT t FROM TransactionTable t " +
            "WHERE t.status = :status " +
            "ORDER BY t.createdAt ASC")
    List<TransactionTable> findByStatusOrderByCreatedAtAsc(
            @Param("status") TransactionTable.TransactionStatus status);

    /**
     * Find all transactions with user details (NO N+1)
     */
    @Query("SELECT DISTINCT t FROM TransactionTable t " +
            "LEFT JOIN FETCH t.userId " +
            "ORDER BY t.createdAt DESC")
    List<TransactionTable> findAllByOrderByCreatedAtDesc();

    /**
     * Find pending transactions older than threshold
     */
    @Query("SELECT t FROM TransactionTable t " +
            "WHERE t.status = com.esport.EsportTournament.model.TransactionTable$TransactionStatus.PENDING " +
            "AND t.createdAt < :cutoff")
    List<TransactionTable> findPendingOlderThan(@Param("cutoff") LocalDateTime cutoff);


    /**
     * Get transaction statistics
     */
    @Query("SELECT " +
            "COUNT(*) as totalTransactions, " +
            "SUM(CASE WHEN t.type = com.esport.EsportTournament.model.TransactionTable$TransactionType.DEPOSIT THEN 1 ELSE 0 END) as depositCount, " +
            "SUM(CASE WHEN t.type = com.esport.EsportTournament.model.TransactionTable$TransactionType.WITHDRAWAL THEN 1 ELSE 0 END) as withdrawalCount, " +
            "SUM(CASE WHEN t.status = com.esport.EsportTournament.model.TransactionTable$TransactionStatus.PENDING THEN 1 ELSE 0 END) as pendingCount, " +
            "SUM(CASE WHEN t.type = com.esport.EsportTournament.model.TransactionTable$TransactionType.DEPOSIT " +
            "          AND t.status = com.esport.EsportTournament.model.TransactionTable$TransactionStatus.COMPLETED THEN t.amount ELSE 0 END) as totalDeposits, " +
            "SUM(CASE WHEN t.type = com.esport.EsportTournament.model.TransactionTable$TransactionType.WITHDRAWAL " +
            "          AND t.status = com.esport.EsportTournament.model.TransactionTable$TransactionStatus.COMPLETED THEN t.amount ELSE 0 END) as totalWithdrawals " +
            "FROM TransactionTable t")
    Optional<Object[]> getTransactionStatistics();


    /**
     * Find transactions by type and status
     */
    @Query("SELECT t FROM TransactionTable t " +
            "WHERE (:type IS NULL OR t.type = :type) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "ORDER BY t.createdAt DESC")
    List<TransactionTable> findByTypeAndStatus(
            @Param("type") TransactionTable.TransactionType type,
            @Param("status") TransactionTable.TransactionStatus status);
}