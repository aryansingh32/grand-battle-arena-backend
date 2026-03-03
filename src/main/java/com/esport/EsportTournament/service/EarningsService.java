package com.esport.EsportTournament.service;

import com.esport.EsportTournament.dto.EarningsReportDTO;
import com.esport.EsportTournament.model.TransactionTable;
import com.esport.EsportTournament.repository.TransactionTableRepo;
import com.esport.EsportTournament.repository.UsersRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Earnings Service — aggregates real transaction data from WalletLedger & TransactionTable
 * to produce accurate financial reports for the admin dashboard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EarningsService {

    private final TransactionTableRepo transactionRepo;
    private final UsersRepo usersRepo;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    // ─────────────────────────────────────────────────────────────
    // Public API — period helpers
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EarningsReportDTO getTodayEarnings() {
        LocalDateTime start = LocalDate.now(IST).atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return buildReport("TODAY", start, end);
    }

    @Transactional(readOnly = true)
    public EarningsReportDTO getWeeklyEarnings() {
        LocalDate today = LocalDate.now(IST);
        LocalDateTime start = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
        LocalDateTime end = start.plusWeeks(1);
        return buildReport("WEEK", start, end);
    }

    @Transactional(readOnly = true)
    public EarningsReportDTO getMonthlyEarnings() {
        LocalDate today = LocalDate.now(IST);
        LocalDateTime start = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        return buildReport("MONTH", start, end);
    }

    @Transactional(readOnly = true)
    public EarningsReportDTO getCustomEarnings(LocalDateTime start, LocalDateTime end) {
        return buildReport("CUSTOM", start, end);
    }

    // ─────────────────────────────────────────────────────────────
    // Core report builder
    // ─────────────────────────────────────────────────────────────

    private EarningsReportDTO buildReport(String period, LocalDateTime start, LocalDateTime end) {
        log.info("📊 Building earnings report: period={}, start={}, end={}", period, start, end);

        // Fetch all completed transactions (we derive everything from the real DB)
        List<TransactionTable> allTransactions = transactionRepo.findAllByOrderByCreatedAtDesc();

        // Filter to date range
        List<TransactionTable> periodTransactions = allTransactions.stream()
                .filter(t -> t.getCreatedAt() != null
                        && !t.getCreatedAt().isBefore(start)
                        && t.getCreatedAt().isBefore(end))
                .collect(Collectors.toList());

        EarningsReportDTO report = new EarningsReportDTO();
        report.setPeriod(period);
        report.setStartDate(start);
        report.setEndDate(end);
        report.setGeneratedAt(LocalDateTime.now(IST));

        // ── Deposits ──
        List<TransactionTable> deposits = periodTransactions.stream()
                .filter(t -> t.getType() == TransactionTable.TransactionType.DEPOSIT
                        && t.getStatus() == TransactionTable.TransactionStatus.COMPLETED)
                .collect(Collectors.toList());

        long totalDeposits = deposits.stream().mapToLong(TransactionTable::getAmount).sum();
        report.setTotalDeposits(totalDeposits);
        report.setDepositCount(deposits.size());
        report.setAverageDepositAmount(deposits.isEmpty() ? 0 : (double) totalDeposits / deposits.size());

        // ── Withdrawals ──
        List<TransactionTable> withdrawals = periodTransactions.stream()
                .filter(t -> t.getType() == TransactionTable.TransactionType.WITHDRAWAL
                        && t.getStatus() == TransactionTable.TransactionStatus.COMPLETED)
                .collect(Collectors.toList());

        long totalWithdrawals = withdrawals.stream().mapToLong(TransactionTable::getAmount).sum();
        report.setTotalWithdrawals(totalWithdrawals);
        report.setWithdrawalCount(withdrawals.size());
        report.setAverageWithdrawalAmount(withdrawals.isEmpty() ? 0 : (double) totalWithdrawals / withdrawals.size());

        // ── Commission & Revenue ──
        double commRate = report.getCommissionRate(); // currently 3%
        long commission = Math.round(totalDeposits * commRate);
        report.setCommissionEarned(commission);
        report.setPlatformRevenue(totalDeposits - totalWithdrawals);
        report.setNetProfit(totalDeposits - totalWithdrawals);

        // ── Transaction Status Counts ──
        report.setTotalTransactions(periodTransactions.size());
        report.setPendingTransactions(periodTransactions.stream()
                .filter(t -> t.getStatus() == TransactionTable.TransactionStatus.PENDING).count());
        report.setCompletedTransactions(periodTransactions.stream()
                .filter(t -> t.getStatus() == TransactionTable.TransactionStatus.COMPLETED).count());
        report.setRejectedTransactions(periodTransactions.stream()
                .filter(t -> t.getStatus() == TransactionTable.TransactionStatus.REJECTED).count());

        // ── User Stats ──
        Set<String> uniqueUserIds = periodTransactions.stream()
                .filter(t -> t.getUserId() != null)
                .map(t -> t.getUserId().getFirebaseUserUID())
                .collect(Collectors.toSet());
        report.setUniqueUsers(uniqueUserIds.size());

        // ── Daily Breakdown ──
        Map<String, List<TransactionTable>> byDate = periodTransactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .collect(Collectors.groupingBy(t ->
                        t.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)));

        List<EarningsReportDTO.DailySummary> dailyBreakdown = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    EarningsReportDTO.DailySummary ds = new EarningsReportDTO.DailySummary();
                    ds.setDate(entry.getKey());

                    long dayDeposits = entry.getValue().stream()
                            .filter(t -> t.getType() == TransactionTable.TransactionType.DEPOSIT
                                    && t.getStatus() == TransactionTable.TransactionStatus.COMPLETED)
                            .mapToLong(TransactionTable::getAmount).sum();
                    long dayWithdrawals = entry.getValue().stream()
                            .filter(t -> t.getType() == TransactionTable.TransactionType.WITHDRAWAL
                                    && t.getStatus() == TransactionTable.TransactionStatus.COMPLETED)
                            .mapToLong(TransactionTable::getAmount).sum();

                    ds.setDeposits(dayDeposits);
                    ds.setWithdrawals(dayWithdrawals);
                    ds.setCommission(Math.round(dayDeposits * commRate));
                    ds.setNetProfit(dayDeposits - dayWithdrawals);
                    ds.setTransactionCount(entry.getValue().size());
                    return ds;
                })
                .collect(Collectors.toList());

        report.setDailyBreakdown(dailyBreakdown);

        // ── Transaction Details (last 50) ──
        List<EarningsReportDTO.TransactionSummary> details = periodTransactions.stream()
                .limit(50)
                .map(t -> {
                    EarningsReportDTO.TransactionSummary ts = new EarningsReportDTO.TransactionSummary();
                    ts.setTransactionUID(t.getTransactionUID());
                    ts.setUserId(t.getUserId() != null ? t.getUserId().getFirebaseUserUID() : "N/A");
                    ts.setUserName(t.getUserId() != null ? t.getUserId().getUserName() : "N/A");
                    ts.setAmount(t.getAmount());
                    ts.setType(t.getType() != null ? t.getType().name() : "UNKNOWN");
                    ts.setStatus(t.getStatus() != null ? t.getStatus().name() : "UNKNOWN");
                    ts.setDate(t.getCreatedAt());
                    ts.setVerifiedBy(t.getVerifiedBy());
                    return ts;
                })
                .collect(Collectors.toList());

        report.setTransactionDetails(details);

        log.info("✅ Earnings report built: deposits=₹{}, withdrawals=₹{}, net=₹{}",
                totalDeposits, totalWithdrawals, totalDeposits - totalWithdrawals);
        return report;
    }

    // ─────────────────────────────────────────────────────────────
    // Excel Export (simple CSV-style for now)
    // ─────────────────────────────────────────────────────────────

    public byte[] generateExcelReport(LocalDateTime start, LocalDateTime end) throws IOException {
        EarningsReportDTO report = buildReport("EXPORT", start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("Grand Battle Arena Earnings Report\n");
        sb.append("Period: ").append(start).append(" to ").append(end).append("\n\n");

        sb.append("Summary\n");
        sb.append("Total Deposits,₹").append(report.getTotalDeposits()).append("\n");
        sb.append("Total Withdrawals,₹").append(report.getTotalWithdrawals()).append("\n");
        sb.append("Net Revenue,₹").append(report.getNetProfit()).append("\n");
        sb.append("Commission Earned,₹").append(report.getCommissionEarned()).append("\n");
        sb.append("Total Transactions,").append(report.getTotalTransactions()).append("\n");
        sb.append("Unique Users,").append(report.getUniqueUsers()).append("\n\n");

        sb.append("Transaction Details\n");
        sb.append("UID,User,Amount,Type,Status,Date\n");
        for (EarningsReportDTO.TransactionSummary ts : report.getTransactionDetails()) {
            sb.append(ts.getTransactionUID()).append(",");
            sb.append(ts.getUserName()).append(",");
            sb.append(ts.getAmount()).append(",");
            sb.append(ts.getType()).append(",");
            sb.append(ts.getStatus()).append(",");
            sb.append(ts.getDate()).append("\n");
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
