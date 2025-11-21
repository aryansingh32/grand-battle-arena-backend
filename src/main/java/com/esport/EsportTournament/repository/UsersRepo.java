package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.Users;
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
public interface UsersRepo extends JpaRepository<Users, Integer> {

    // ========== OPTIMIZED QUERIES ==========

    /**
     * Find by Firebase UID with query hint for caching
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Users> findByFirebaseUserUID(String firebaseUID);

    /**
     * Check if user exists (uses covering index)
     */
    boolean existsByFirebaseUserUID(String firebaseUserUID);

    /**
     * Count users by status (uses covering index)
     */
    long countByStatus(Users.UserStatus status);

    /**
     * Find users by role with caching
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<Users> findByRole(Users.UserRole role);

    /**
     * Find users by status with caching
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<Users> findByStatus(Users.UserStatus status);

    /**
     * Search users by username (case-insensitive, indexed)
     */
    @Query("SELECT u FROM Users u " +
            "WHERE LOWER(u.userName) LIKE LOWER(CONCAT('%', :username, '%')) " +
            "ORDER BY u.userName ASC")
    List<Users> findByUserNameContainingIgnoreCase(@Param("username") String username);

    /**
     * Check if email exists (uses unique index)
     */
    boolean existsByEmail(String email);

    /**
     * Check if username exists (uses unique index)
     */
    boolean existsByUserName(String userName);

    /**
     * Find users created within date range (uses index)
     */
    @Query("SELECT u FROM Users u " +
            "WHERE u.createdAt BETWEEN :start AND :end " +
            "ORDER BY u.createdAt DESC")
    List<Users> findByCreatedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Find multiple users by Firebase UIDs (batch operation)
     */
    @Query("SELECT u FROM Users u WHERE u.firebaseUserUID IN :firebaseUIDs")
    List<Users> findByFirebaseUserUIDIn(@Param("firebaseUIDs") List<String> firebaseUIDs);

    /**
     * Find all users ordered by creation date
     */
    @Query("SELECT u FROM Users u ORDER BY u.createdAt DESC")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<Users> findAllByOrderByCreatedAtDesc();

    /**
     * Find user by device token
     */
    Optional<Users> findByDeviceToken(String deviceToken);

    /**
     * Find users with valid device tokens (for notifications)
     */
    @Query("SELECT u FROM Users u " +
            "WHERE u.deviceToken IS NOT NULL " +
            "AND u.status = 'ACTIVE'")
    List<Users> findByDeviceTokenIsNotNull();

    /**
     * Find user by email
     */
    Optional<Users> findByEmail(String email);

    /**
     * Get user statistics
     */
    @Query("SELECT " +
            "COUNT(*) as totalUsers, " +
            "SUM(CASE WHEN u.status = 'ACTIVE' THEN 1 ELSE 0 END) as activeUsers, " +
            "SUM(CASE WHEN u.status = 'BANNED' THEN 1 ELSE 0 END) as bannedUsers, " +
            "SUM(CASE WHEN u.role = 'ADMIN' THEN 1 ELSE 0 END) as adminUsers " +
            "FROM Users u")
    Optional<Object[]> getUserStatistics();

    /**
     * Find active users last active within timeframe
     */
    @Query("SELECT u FROM Users u " +
            "WHERE u.status = 'ACTIVE' " +
            "AND u.lastActiveAt >= :since " +
            "ORDER BY u.lastActiveAt DESC")
    List<Users> findActiveUsersSince(@Param("since") LocalDateTime since);

    /**
     * Find inactive users (for cleanup/reminder emails)
     */
    @Query("SELECT u FROM Users u " +
            "WHERE u.status = 'ACTIVE' " +
            "AND u.lastActiveAt < :before " +
            "ORDER BY u.lastActiveAt ASC")
    List<Users> findInactiveUsers(@Param("before") LocalDateTime before);
}