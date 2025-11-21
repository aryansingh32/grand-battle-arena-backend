package com.esport.EsportTournament.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ NEW: Earnings Report DTO
 * Complete financial reporting structure
 */
@Data
public class EarningsReportDTO {

    // Period Information
    private String period;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime generatedAt;

    // Deposit Summary
    private long totalDeposits;
    private long depositCount;
    private double averageDepositAmount;

    // Withdrawal Summary
    private long totalWithdrawals;
    private long withdrawalCount;
    private double averageWithdrawalAmount;

    // Commission & Fees
    private long commissionEarned;
    private double commissionRate = 0.03; // 3%

    // Platform Revenue
    private long platformRevenue;
    private long netProfit;

    // Transaction Statistics
    private long totalTransactions;
    private long pendingTransactions;
    private long completedTransactions;
    private long rejectedTransactions;

    // User Statistics
    private long uniqueUsers;
    private long newUsersInPeriod;

    // Tournament Revenue
    private long tournamentEntryRevenue;
    private long tournamentPrizePayout;

    // Detailed Transaction List
    private List<TransactionSummary> transactionDetails = new ArrayList<>();

    // Daily Breakdown
    private List<DailySummary> dailyBreakdown = new ArrayList<>();

    @Data
    public static class TransactionSummary {
        private String transactionUID;
        private String userId;
        private String userName;
        private long amount;
        private String type;
        private String status;
        private LocalDateTime date;
        private String verifiedBy;
        private String notes;
    }

    @Data
    public static class DailySummary {
        private String date;
        private long deposits;
        private long withdrawals;
        private long commission;
        private long netProfit;
        private int transactionCount;
    }
}