
package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.Tournaments;
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
public interface TournamentRepo extends JpaRepository<Tournaments, Integer> {

    // ========== OPTIMIZED QUERIES WITH FETCH JOINS ==========

    /**
     * Find tournament with all related data in single query (NO N+1 problem)
     */
//    @Query("SELECT DISTINCT t FROM Tournaments t " +
//            "LEFT JOIN FETCH t.slots " +
//            "WHERE t.id = :id")
//    Optional<Tournaments> findByIdWithSlots(@Param("id") int id);

    /**
     * Find tournaments by status with query hints for performance
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<Tournaments> findByStatus(Tournaments.TournamentStatus status);

    /**
     * Find upcoming tournaments ordered by start time
     */
    @Query("SELECT t FROM Tournaments t " +
            "WHERE t.status = :status " +
            "ORDER BY t.startTime ASC")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<Tournaments> findByStatusOrderByStartTimeAsc(@Param("status") Tournaments.TournamentStatus status);

    /**
     * Find tournaments by game type with pagination support
     */
    List<Tournaments> findByGameOrderByStartTimeDesc(String game);

    /**
     * Find tournaments by team size
     */
    List<Tournaments> findByTeamSizeOrderByStartTimeDesc(String teamSize);

    /**
     * Find tournaments starting within date range
     */
    @Query("SELECT t FROM Tournaments t " +
            "WHERE t.startTime BETWEEN :start AND :end " +
            "ORDER BY t.startTime ASC")
    List<Tournaments> findByStartTimeBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Find tournaments that need to be started (for scheduler)
     * CRITICAL: Uses index on (status, start_time)
     */
    @Query("SELECT t FROM Tournaments t " +
            "WHERE t.status = :status " +
            "AND t.startTime <= :dateTime " +
            "ORDER BY t.startTime ASC")
    List<Tournaments> findByStatusAndStartTimeBefore(
            @Param("status") Tournaments.TournamentStatus status,
            @Param("dateTime") LocalDateTime dateTime);

    /**
     * Count tournaments by status (for analytics)
     * Uses covering index for performance
     */
    long countByStatus(Tournaments.TournamentStatus status);

    /**
     * Find tournaments by entry fee range
     */
    @Query("SELECT t FROM Tournaments t " +
            "WHERE t.entryFees BETWEEN :minFee AND :maxFee " +
            "ORDER BY t.startTime DESC")
    List<Tournaments> findByEntryFeesBetween(
            @Param("minFee") int minFee,
            @Param("maxFee") int maxFee);

    /**
     * Find tournaments by prize pool range
     */
    @Query("SELECT t FROM Tournaments t " +
            "WHERE t.prizePool BETWEEN :minPrize AND :maxPrize " +
            "ORDER BY t.prizePool DESC")
    List<Tournaments> findByPrizePoolBetween(
            @Param("minPrize") int minPrize,
            @Param("maxPrize") int maxPrize);

    /**
     * Find all tournaments ordered by start time descending
     */
    @Query("SELECT t FROM Tournaments t ORDER BY t.startTime DESC")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<Tournaments> findAllByOrderByStartTimeDesc();

    /**
     * Find tournaments with available slots
     * OPTIMIZED: Uses subquery with indexed columns
     */
//    @Query("SELECT DISTINCT t FROM Tournaments t " +
//            "WHERE t.status = :status " +
//            "AND EXISTS (" +
//            "  SELECT 1 FROM Slots s " +
//            "  WHERE s.tournaments.id = t.id " +
//            "  AND s.status = 'AVAILABLE'" +
//            ") " +
//            "ORDER BY t.startTime ASC")
//    List<Tournaments> findTournamentsWithAvailableSlots(
//            @Param("status") Tournaments.TournamentStatus status);

    /**
     * Find popular tournaments (with high booking rate)
     */
//    @Query("SELECT t, " +
//            "COUNT(s) as totalSlots, " +
//            "SUM(CASE WHEN s.status = 'BOOKED' THEN 1 ELSE 0 END) as bookedSlots " +
//            "FROM Tournaments t " +
//            "LEFT JOIN t.slots s " +
//            "WHERE t.status = :status " +
//            "GROUP BY t " +
//            "HAVING SUM(CASE WHEN s.status = 'BOOKED' THEN 1 ELSE 0 END) > 0 " +
//            "ORDER BY (SUM(CASE WHEN s.status = 'BOOKED' THEN 1 ELSE 0 END) * 1.0 / COUNT(s)) DESC")
//    List<Object[]> findPopularTournaments(@Param("status") Tournaments.TournamentStatus status);

    /**
     * Search tournaments by name (case-insensitive)
     */
    @Query("SELECT t FROM Tournaments t " +
            "WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "ORDER BY t.startTime DESC")
    List<Tournaments> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Find tournaments with slot statistics
     * ONE QUERY to get all data (eliminates N+1)
     */
//    @Query("SELECT t, " +
//            "COUNT(s) as totalSlots, " +
//            "SUM(CASE WHEN s.status = 'BOOKED' THEN 1 ELSE 0 END) as bookedSlots, " +
//            "SUM(CASE WHEN s.status = 'AVAILABLE' THEN 1 ELSE 0 END) as availableSlots " +
//            "FROM Tournaments t " +
//            "LEFT JOIN t.slots s " +
//            "WHERE t.id = :tournamentId " +
//            "GROUP BY t")
//    Optional<Object[]> findTournamentWithStats(@Param("tournamentId") int tournamentId);

    List<Tournaments> findByStatusAndStartTimeBetween(Tournaments.TournamentStatus tournamentStatus, LocalDateTime now, LocalDateTime in15Minutes);
}