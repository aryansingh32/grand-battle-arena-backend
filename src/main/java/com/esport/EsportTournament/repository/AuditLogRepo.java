package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ NEW: Audit Log Repository
 * - Optimized queries with indexes
 * - Support for audit trail retrieval
 */
@Repository
public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {

    /**
     * Find recent logs by category
     */
    List<AuditLog> findTop100ByCategoryOrderByTimestampDesc(String category);

    /**
     * Find recent logs by user
     */
    List<AuditLog> findTop100ByUserIdOrderByTimestampDesc(String userId);

    /**
     * Find recent logs
     */
    List<AuditLog> findTop100ByOrderByTimestampDesc();

    /**
     * Find logs by action
     */
    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    /**
     * Find logs within date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    List<AuditLog> findByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count logs by category
     */
    long countByCategory(String category);

    /**
     * Count logs by user
     */
    long countByUserId(String userId);

    /**
     * Find logs by user and category
     */
    List<AuditLog> findByUserIdAndCategoryOrderByTimestampDesc(String userId, String category);

    /**
     * Find logs by IP address
     */
    List<AuditLog> findByIpAddressOrderByTimestampDesc(String ipAddress);
}