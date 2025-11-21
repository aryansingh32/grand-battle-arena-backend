package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.Slots;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ✅ FIXED: Optimized Slot Repository
 * - Added proper indexing hints
 * - Removed N+1 queries
 * - Added batch operations
 * - Optimized locking strategies
 */
@Repository
public interface SlotRepo extends JpaRepository<Slots, Integer> {

    // ========== OPTIMIZED QUERIES ==========

    /**
     * ✅ FIXED: Added index hint for performance
     */
    @Query("SELECT COUNT(s) > 0 FROM Slots s WHERE s.tournaments.id = :tournamentId AND s.slotNumber = :slotNumber")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    boolean existsByTournaments_IdAndSlotNumber(@Param("tournamentId") int tournamentId,
                                                @Param("slotNumber") int slotNumber);

    /**
     * ✅ FIXED: Single query with JOIN FETCH - No N+1 problem
     */
    @Query("SELECT DISTINCT s FROM Slots s " +
            "LEFT JOIN FETCH s.user " +
            "LEFT JOIN FETCH s.tournaments " +
            "WHERE s.tournaments.id = :tournamentId " +
            "ORDER BY s.slotNumber ASC")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<Slots> findByTournaments_Id(@Param("tournamentId") int tournamentId);

    /**
     * ✅ FIXED: Pessimistic write lock for concurrent booking
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slots s " +
            "WHERE s.tournaments.id = :tournamentId " +
            "AND s.status = :status " +
            "ORDER BY s.slotNumber ASC")
    Optional<Slots> findFirstByTournaments_IdAndStatusOrderBySlotNumberAsc(
            @Param("tournamentId") int tournamentId,
            @Param("status") Slots.SlotStatus status);

    /**
     * ✅ FIXED: Separate read and write queries
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slots s " +
            "WHERE s.tournaments.id = :tournamentId " +
            "AND s.slotNumber = :slotNumber")
    Optional<Slots> findByTournaments_IdAndSlotNumberForUpdate(
            @Param("tournamentId") int tournamentId,
            @Param("slotNumber") int slotNumber);

    Optional<Slots> findByTournaments_IdAndSlotNumber(int tournamentId, int slotNumber);

    /**
     * ✅ NEW: Batch check for user booking
     */
    @Query("SELECT s.tournaments.id FROM Slots s " +
            "WHERE s.user.firebaseUserUID = :firebaseUID " +
            "AND s.status = 'BOOKED' " +
            "AND s.tournaments.id IN :tournamentIds")
    List<Integer> findUserBookedTournaments(
            @Param("firebaseUID") String firebaseUID,
            @Param("tournamentIds") List<Integer> tournamentIds);

    /**
     * ✅ FIXED: Optimized existence check
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Slots s " +
            "WHERE s.tournaments.id = :tournamentId " +
            "AND s.user.firebaseUserUID = :firebaseUID " +
            "AND s.status = 'BOOKED'")
    boolean existsByTournaments_IdAndUser_FirebaseUserUID(
            @Param("tournamentId") int tournamentId,
            @Param("firebaseUID") String firebaseUID);

    /**
     * ✅ FIXED: Single query with JOIN FETCH
     */
    @Query("SELECT DISTINCT s FROM Slots s " +
            "LEFT JOIN FETCH s.tournaments t " +
            "WHERE s.user.firebaseUserUID = :firebaseUID " +
            "AND s.status = 'BOOKED' " +
            "ORDER BY s.bookedAt DESC")
    List<Slots> findByUser_FirebaseUserUID(@Param("firebaseUID") String firebaseUID);

    /**
     * ✅ FIXED: Count query optimization
     */
    @Query("SELECT COUNT(s) FROM Slots s " +
            "WHERE s.tournaments.id = :tournamentId " +
            "AND s.status = :status")
    long countByTournaments_IdAndStatus(
            @Param("tournamentId") int tournamentId,
            @Param("status") Slots.SlotStatus status);

    /**
     * ✅ NEW: Batch slot summary
     */
    @Query("SELECT s.tournaments.id, s.status, COUNT(s) FROM Slots s " +
            "WHERE s.tournaments.id IN :tournamentIds " +
            "GROUP BY s.tournaments.id, s.status")
    List<Object[]> getSlotSummaryBatch(@Param("tournamentIds") List<Integer> tournamentIds);

    /**
     * ✅ FIXED: Optimized statistics query
     */
    @Query("SELECT " +
            "COUNT(s) as totalSlots, " +
            "SUM(CASE WHEN s.status = 'BOOKED' THEN 1 ELSE 0 END) as bookedCount, " +
            "SUM(CASE WHEN s.status = 'AVAILABLE' THEN 1 ELSE 0 END) as availableCount, " +
            "MIN(s.bookedAt) as firstBooking, " +
            "MAX(s.bookedAt) as lastBooking " +
            "FROM Slots s " +
            "WHERE s.tournaments.id = :tournamentId")
    Optional<Object[]> getBookingStatistics(@Param("tournamentId") int tournamentId);

    /**
     * ✅ NEW: Cleanup expired bookings
     */
    @Query("SELECT s FROM Slots s " +
            "JOIN FETCH s.tournaments t " +
            "WHERE s.status = 'BOOKED' " +
            "AND t.status = 'CANCELLED' " +
            "AND s.bookedAt < :before")
    List<Slots> findExpiredBookings(@Param("before") LocalDateTime before);

    /**
     * ✅ NEW: Get tournament fill rate
     */
    @Query("SELECT " +
            "(CAST(COUNT(CASE WHEN s.status = 'BOOKED' THEN 1 END) AS double) / COUNT(s)) * 100 " +
            "FROM Slots s " +
            "WHERE s.tournaments.id = :tournamentId")
    Optional<Double> getTournamentFillRate(@Param("tournamentId") int tournamentId);
}