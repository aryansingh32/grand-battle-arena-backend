package com.esport.EsportTournament.service;

import com.esport.EsportTournament.model.Tournaments;
import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced Analytics Service
 * Provides comprehensive dashboard metrics and insights
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TournamentRepo tournamentRepo;
    private final UsersRepo usersRepo;
    private final WalletRepo walletRepo;
    private final TransactionTableRepo transactionRepo;
    private final SlotRepo slotRepo;

    /**
     * Get comprehensive admin dashboard metrics
     * Cached for 5 minutes to reduce database load
     */
    @Cacheable(value = "dashboardMetrics", unless = "#result == null")
    @Transactional(readOnly = true)
    public DashboardMetrics getAdminDashboard() {
        log.info("📊 Generating admin dashboard metrics");

        DashboardMetrics metrics = new DashboardMetrics();

        // User metrics
        metrics.setTotalUsers(usersRepo.count());
        metrics.setActiveUsers(usersRepo.countByStatus(Users.UserStatus.ACTIVE));
        metrics.setBannedUsers(usersRepo.countByStatus(Users.UserStatus.BANNED));
        metrics.setNewUsersToday(countNewUsersToday());
        metrics.setNewUsersThisWeek(countNewUsersThisWeek());
        metrics.setNewUsersThisMonth(countNewUsersThisMonth());

        // Tournament metrics
        metrics.setTotalTournaments(tournamentRepo.count());
        metrics.setUpcomingTournaments(tournamentRepo.countByStatus(Tournaments.TournamentStatus.UPCOMING));
        metrics.setOngoingTournaments(tournamentRepo.countByStatus(Tournaments.TournamentStatus.ONGOING));
        metrics.setCompletedTournaments(tournamentRepo.countByStatus(Tournaments.TournamentStatus.COMPLETED));

        // Financial metrics
        metrics.setTotalCoinsInCirculation(calculateTotalCoins());
        metrics.setPendingWithdrawals(transactionRepo.countByStatus(
                com.esport.EsportTournament.model.TransactionTable.TransactionStatus.PENDING));
        metrics.setTotalRevenue(calculateTotalRevenue());
        metrics.setRevenueToday(calculateRevenueToday());

        // Engagement metrics
        metrics.setAverageBookingsPerTournament(calculateAvgBookingsPerTournament());
        metrics.setTotalSlotsBookedToday(countSlotsBookedToday());
        metrics.setMostPopularGame(findMostPopularGame());

        // Growth metrics
        metrics.setUserGrowthRate(calculateUserGrowthRate());
        metrics.setRevenueGrowthRate(calculateRevenueGrowthRate());

        // Chart data
        metrics.setUserGrowthChart(generateUserGrowthChart());
        metrics.setRevenueChart(generateRevenueChart());
        metrics.setTournamentDistribution(generateTournamentDistribution());

        metrics.setGeneratedAt(LocalDateTime.now());

        log.info("✅ Dashboard metrics generated successfully");
        return metrics;
    }

    /**
     * Get detailed tournament analytics
     */
    @Transactional(readOnly = true)
    public TournamentAnalytics getTournamentAnalytics(int tournamentId) {
        log.info("📊 Generating analytics for tournament {}", tournamentId);

        TournamentAnalytics analytics = new TournamentAnalytics();

        // Basic metrics
        long totalSlots = slotRepo.countByTournaments_IdAndStatus(
                tournamentId, com.esport.EsportTournament.model.Slots.SlotStatus.BOOKED) +
                slotRepo.countByTournaments_IdAndStatus(
                        tournamentId, com.esport.EsportTournament.model.Slots.SlotStatus.AVAILABLE);

        long bookedSlots = slotRepo.countByTournaments_IdAndStatus(
                tournamentId, com.esport.EsportTournament.model.Slots.SlotStatus.BOOKED);

        analytics.setTotalParticipants((int) bookedSlots);
        analytics.setTotalSlots((int) totalSlots);
        analytics.setBookingRate(totalSlots > 0 ? (bookedSlots * 100.0 / totalSlots) : 0);

        // Revenue metrics
        var tournament = tournamentRepo.findById(tournamentId).orElse(null);
        if (tournament != null) {
            analytics.setRevenueGenerated(bookedSlots * tournament.getEntryFees());
            analytics.setPotentialRevenue(totalSlots * tournament.getEntryFees());
        }

        // Booking timeline
        analytics.setBookingTimeline(generateBookingTimeline(tournamentId));

        // Participant demographics (game preferences, etc.)
        analytics.setParticipantStats(generateParticipantStats(tournamentId));

        return analytics;
    }

    /**
     * Get user engagement metrics
     */
    @Transactional(readOnly = true)
    public UserEngagementMetrics getUserEngagementMetrics() {
        UserEngagementMetrics metrics = new UserEngagementMetrics();

        // Active users by timeframe
        metrics.setDailyActiveUsers(countActiveUsersInLast(1));
        metrics.setWeeklyActiveUsers(countActiveUsersInLast(7));
        metrics.setMonthlyActiveUsers(countActiveUsersInLast(30));

        // Engagement rates
        metrics.setAverageSessionsPerUser(calculateAvgSessionsPerUser());
        metrics.setAverageTournamentsPerUser(calculateAvgTournamentsPerUser());

        // Retention metrics
        metrics.setRetentionRate7Days(calculateRetentionRate(7));
        metrics.setRetentionRate30Days(calculateRetentionRate(30));

        return metrics;
    }

    /**
     * Get financial analytics
     */
    @Transactional(readOnly = true)
    public FinancialAnalytics getFinancialAnalytics() {
        FinancialAnalytics analytics = new FinancialAnalytics();

        // Total metrics
        analytics.setTotalDeposits(calculateTotalDeposits());
        analytics.setTotalWithdrawals(calculateTotalWithdrawals());
        analytics.setNetRevenue(analytics.getTotalDeposits() - analytics.getTotalWithdrawals());

        // Time-based metrics
        analytics.setDepositsToday(calculateDepositsToday());
        analytics.setWithdrawalsToday(calculateWithdrawalsToday());

        // Average metrics
        analytics.setAverageDepositAmount(calculateAverageDepositAmount());
        analytics.setAverageWithdrawalAmount(calculateAverageWithdrawalAmount());

        // Pending transactions
        analytics.setPendingDeposits(countPendingDeposits());
        analytics.setPendingWithdrawals(countPendingWithdrawals());

        // Charts
        analytics.setTransactionTrendChart(generateTransactionTrendChart());

        return analytics;
    }

    // ========== Private Helper Methods ==========

    private long countNewUsersToday() {
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return usersRepo.findByCreatedAtBetween(startOfDay, LocalDateTime.now()).size();
    }

    private long countNewUsersThisWeek() {
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        return usersRepo.findByCreatedAtBetween(startOfWeek, LocalDateTime.now()).size();
    }

    private long countNewUsersThisMonth() {
        LocalDateTime startOfMonth = LocalDateTime.now().minusDays(30);
        return usersRepo.findByCreatedAtBetween(startOfMonth, LocalDateTime.now()).size();
    }

    private long calculateTotalCoins() {
        return walletRepo.findAll().stream()
                .mapToLong(wallet -> wallet.getCoins())
                .sum();
    }

    private long calculateTotalRevenue() {
        // Sum of all completed tournament entry fees
        return tournamentRepo.findAll().stream()
                .filter(t -> t.getStatus() == Tournaments.TournamentStatus.COMPLETED)
                .mapToLong(t -> {
                    long bookedSlots = slotRepo.countByTournaments_IdAndStatus(
                            t.getId(), com.esport.EsportTournament.model.Slots.SlotStatus.BOOKED);
                    return bookedSlots * t.getEntryFees();
                })
                .sum();
    }

    private long calculateRevenueToday() {
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return slotRepo.findAll().stream()
                .filter(slot -> slot.getBookedAt() != null && slot.getBookedAt().isAfter(startOfDay))
                .mapToLong(slot -> slot.getTournaments().getEntryFees())
                .sum();
    }

    private double calculateAvgBookingsPerTournament() {
        List<Integer> tournamentIds = tournamentRepo.findAll().stream()
                .map(com.esport.EsportTournament.model.Tournaments::getId)
                .collect(Collectors.toList());

        if (tournamentIds.isEmpty()) return 0.0;

        long totalBookings = tournamentIds.stream()
                .mapToLong(id -> slotRepo.countByTournaments_IdAndStatus(
                        id, com.esport.EsportTournament.model.Slots.SlotStatus.BOOKED))
                .sum();

        return (double) totalBookings / tournamentIds.size();
    }

    private long countSlotsBookedToday() {
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return slotRepo.findAll().stream()
                .filter(slot -> slot.getBookedAt() != null && slot.getBookedAt().isAfter(startOfDay))
                .count();
    }

    private String findMostPopularGame() {
        Map<String, Long> gameCounts = tournamentRepo.findAll().stream()
                .collect(Collectors.groupingBy(
                        com.esport.EsportTournament.model.Tournaments::getGame,
                        Collectors.counting()
                ));

        return gameCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    private double calculateUserGrowthRate() {
        long thisMonth = countNewUsersThisMonth();
        long lastMonth = countNewUsersBetween(60, 30);

        if (lastMonth == 0) return thisMonth > 0 ? 100.0 : 0.0;

        return ((double) (thisMonth - lastMonth) / lastMonth) * 100;
    }

    private long countNewUsersBetween(int daysAgo, int untilDaysAgo) {
        LocalDateTime start = LocalDateTime.now().minusDays(daysAgo);
        LocalDateTime end = LocalDateTime.now().minusDays(untilDaysAgo);
        return usersRepo.findByCreatedAtBetween(start, end).size();
    }

    private double calculateRevenueGrowthRate() {
        // Implement revenue growth rate calculation
        return 0.0; // Placeholder
    }

    private List<Map<String, Object>> generateUserGrowthChart() {
        List<Map<String, Object>> chartData = new ArrayList<>();

        for (int i = 30; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            LocalDateTime nextDay = date.plusDays(1);

            long count = usersRepo.findByCreatedAtBetween(
                    date.truncatedTo(ChronoUnit.DAYS),
                    nextDay.truncatedTo(ChronoUnit.DAYS)
            ).size();

            chartData.add(Map.of(
                    "date", date.toLocalDate().toString(),
                    "count", count
            ));
        }

        return chartData;
    }

    private List<Map<String, Object>> generateRevenueChart() {
        // Generate revenue chart data for last 30 days
        List<Map<String, Object>> chartData = new ArrayList<>();

        for (int i = 30; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            LocalDateTime nextDay = date.plusDays(1);

            long revenue = slotRepo.findAll().stream()
                    .filter(slot -> slot.getBookedAt() != null)
                    .filter(slot -> slot.getBookedAt().isAfter(date.truncatedTo(ChronoUnit.DAYS)))
                    .filter(slot -> slot.getBookedAt().isBefore(nextDay.truncatedTo(ChronoUnit.DAYS)))
                    .mapToLong(slot -> slot.getTournaments().getEntryFees())
                    .sum();

            chartData.add(Map.of(
                    "date", date.toLocalDate().toString(),
                    "revenue", revenue
            ));
        }

        return chartData;
    }

    private Map<String, Long> generateTournamentDistribution() {
        return tournamentRepo.findAll().stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus().name(),
                        Collectors.counting()
                ));
    }

    private List<Map<String, Object>> generateBookingTimeline(int tournamentId) {
        // Generate booking timeline for specific tournament
        return new ArrayList<>(); // Implement based on booking timestamps
    }

    private Map<String, Object> generateParticipantStats(int tournamentId) {
        return Map.of(
                "totalParticipants", slotRepo.countByTournaments_IdAndStatus(
                        tournamentId, com.esport.EsportTournament.model.Slots.SlotStatus.BOOKED)
        );
    }

    private long countActiveUsersInLast(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        // This would require a last_active_at field in Users table
        return usersRepo.countByStatus(Users.UserStatus.ACTIVE);
    }

    private double calculateAvgSessionsPerUser() {
        // Requires session tracking
        return 0.0; // Placeholder
    }

    private double calculateAvgTournamentsPerUser() {
        long totalUsers = usersRepo.count();
        if (totalUsers == 0) return 0.0;

        long totalParticipations = slotRepo.findAll().stream()
                .filter(slot -> slot.getUser() != null)
                .map(slot -> slot.getUser().getId())
                .distinct()
                .count();

        return (double) totalParticipations / totalUsers;
    }

    private double calculateRetentionRate(int days) {
        // Requires tracking user activity
        return 0.0; // Placeholder
    }

    private long calculateTotalDeposits() {
        return transactionRepo.findAll().stream()
                .filter(t -> t.getType() == com.esport.EsportTournament.model.TransactionTable.TransactionType.DEPOSIT)
                .filter(t -> t.getStatus() == com.esport.EsportTournament.model.TransactionTable.TransactionStatus.COMPLETED)
                .mapToLong(com.esport.EsportTournament.model.TransactionTable::getAmount)
                .sum();
    }

    private long calculateTotalWithdrawals() {
        return transactionRepo.findAll().stream()
                .filter(t -> t.getType() == com.esport.EsportTournament.model.TransactionTable.TransactionType.WITHDRAWAL)
                .filter(t -> t.getStatus() == com.esport.EsportTournament.model.TransactionTable.TransactionStatus.COMPLETED)
                .mapToLong(com.esport.EsportTournament.model.TransactionTable::getAmount)
                .sum();
    }

    private long calculateDepositsToday() {
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return transactionRepo.findAll().stream()
                .filter(t -> t.getType() == com.esport.EsportTournament.model.TransactionTable.TransactionType.DEPOSIT)
                .filter(t -> t.getCreatedAt().isAfter(startOfDay))
                .mapToLong(com.esport.EsportTournament.model.TransactionTable::getAmount)
                .sum();
    }

    private long calculateWithdrawalsToday() {
        LocalDateTime startOfDay = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return transactionRepo.findAll().stream()
                .filter(t -> t.getType() == com.esport.EsportTournament.model.TransactionTable.TransactionType.WITHDRAWAL)
                .filter(t -> t.getCreatedAt().isAfter(startOfDay))
                .mapToLong(com.esport.EsportTournament.model.TransactionTable::getAmount)
                .sum();
    }

    private double calculateAverageDepositAmount() {
        return transactionRepo.findAll().stream()
                .filter(t -> t.getType() == com.esport.EsportTournament.model.TransactionTable.TransactionType.DEPOSIT)
                .mapToLong(com.esport.EsportTournament.model.TransactionTable::getAmount)
                .average()
                .orElse(0.0);
    }

    private double calculateAverageWithdrawalAmount() {
        return transactionRepo.findAll().stream()
                .filter(t -> t.getType() == com.esport.EsportTournament.model.TransactionTable.TransactionType.WITHDRAWAL)
                .mapToLong(com.esport.EsportTournament.model.TransactionTable::getAmount)
                .average()
                .orElse(0.0);
    }

    private long countPendingDeposits() {
        return transactionRepo.findAll().stream()
                .filter(t -> t.getType() == com.esport.EsportTournament.model.TransactionTable.TransactionType.DEPOSIT)
                .filter(t -> t.getStatus() == com.esport.EsportTournament.model.TransactionTable.TransactionStatus.PENDING)
                .count();
    }

    private long countPendingWithdrawals() {
        return transactionRepo.findAll().stream()
                .filter(t -> t.getType() == com.esport.EsportTournament.model.TransactionTable.TransactionType.WITHDRAWAL)
                .filter(t -> t.getStatus() == com.esport.EsportTournament.model.TransactionTable.TransactionStatus.PENDING)
                .count();
    }

    private List<Map<String, Object>> generateTransactionTrendChart() {
        return new ArrayList<>(); // Implement transaction trend chart
    }

    // ========== DTOs ==========

    @Data
    public static class DashboardMetrics {
        // User metrics
        private long totalUsers;
        private long activeUsers;
        private long bannedUsers;
        private long newUsersToday;
        private long newUsersThisWeek;
        private long newUsersThisMonth;

        // Tournament metrics
        private long totalTournaments;
        private long upcomingTournaments;
        private long ongoingTournaments;
        private long completedTournaments;

        // Financial metrics
        private long totalCoinsInCirculation;
        private long pendingWithdrawals;
        private long totalRevenue;
        private long revenueToday;

        // Engagement metrics
        private double averageBookingsPerTournament;
        private long totalSlotsBookedToday;
        private String mostPopularGame;

        // Growth metrics
        private double userGrowthRate;
        private double revenueGrowthRate;

        // Chart data
        private List<Map<String, Object>> userGrowthChart;
        private List<Map<String, Object>> revenueChart;
        private Map<String, Long> tournamentDistribution;

        private LocalDateTime generatedAt;
    }

    @Data
    public static class TournamentAnalytics {
        private int totalParticipants;
        private int totalSlots;
        private double bookingRate;
        private long revenueGenerated;
        private long potentialRevenue;
        private List<Map<String, Object>> bookingTimeline;
        private Map<String, Object> participantStats;
    }

    @Data
    public static class UserEngagementMetrics {
        private long dailyActiveUsers;
        private long weeklyActiveUsers;
        private long monthlyActiveUsers;
        private double averageSessionsPerUser;
        private double averageTournamentsPerUser;
        private double retentionRate7Days;
        private double retentionRate30Days;
    }

    @Data
    public static class FinancialAnalytics {
        private long totalDeposits;
        private long totalWithdrawals;
        private long netRevenue;
        private long depositsToday;
        private long withdrawalsToday;
        private double averageDepositAmount;
        private double averageWithdrawalAmount;
        private long pendingDeposits;
        private long pendingWithdrawals;
        private List<Map<String, Object>> transactionTrendChart;
    }
}