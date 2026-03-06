package com.esport.EsportTournament.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom business metrics tracked via Micrometer (exposed on /actuator/prometheus).
 *
 * Tracks:
 * - Bookings created/failed/cancelled
 * - Deposits submitted
 * - Withdrawals submitted
 * - Active WebSocket connections (gauge)
 * - Tournament views
 * - API errors by type
 *
 * View in Grafana or any Prometheus-compatible dashboard.
 */
@Slf4j
@Service
public class MetricsService {

    private final MeterRegistry registry;

    // ─── Counters ───
    private final Counter bookingsCreated;
    private final Counter bookingsFailed;
    private final Counter bookingsCancelled;
    private final Counter depositsSubmitted;
    private final Counter withdrawalsSubmitted;
    private final Counter tournamentsViewed;
    private final Counter signInsSuccessful;
    private final Counter signInsFailed;
    private final Counter userRegistrations;
    private final Counter notificationsSent;
    private final Counter walletCredits;
    private final Counter walletDebits;
    private final Counter tournamentsCreated;

    // ─── Gauges ───
    private final AtomicInteger activeWebSocketConnections = new AtomicInteger(0);

    // ─── Timers ───
    private final Timer bookingProcessingTime;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;

        // Booking metrics
        this.bookingsCreated = Counter.builder("app.bookings.created")
                .description("Total bookings successfully created")
                .register(registry);
        this.bookingsFailed = Counter.builder("app.bookings.failed")
                .description("Total booking attempts that failed")
                .register(registry);
        this.bookingsCancelled = Counter.builder("app.bookings.cancelled")
                .description("Total bookings cancelled by users")
                .register(registry);

        // Transaction metrics
        this.depositsSubmitted = Counter.builder("app.transactions.deposit")
                .description("Total deposit transactions submitted")
                .register(registry);
        this.withdrawalsSubmitted = Counter.builder("app.transactions.withdrawal")
                .description("Total withdrawal transactions submitted")
                .register(registry);

        // Engagement metrics
        this.tournamentsViewed = Counter.builder("app.tournaments.viewed")
                .description("Total tournament detail views")
                .register(registry);
        this.signInsSuccessful = Counter.builder("app.auth.signin.success")
                .description("Successful sign-ins")
                .register(registry);
        this.signInsFailed = Counter.builder("app.auth.signin.failed")
                .description("Failed sign-in attempts")
                .register(registry);

        // User metrics
        this.userRegistrations = Counter.builder("app.users.registered")
                .description("Total new user registrations")
                .register(registry);

        // Notification metrics
        this.notificationsSent = Counter.builder("app.notifications.sent")
                .description("Total push notifications dispatched")
                .register(registry);

        // Wallet metrics
        this.walletCredits = Counter.builder("app.wallet.credits")
                .description("Total wallet credit operations")
                .register(registry);
        this.walletDebits = Counter.builder("app.wallet.debits")
                .description("Total wallet debit operations")
                .register(registry);

        // Tournament management
        this.tournamentsCreated = Counter.builder("app.tournaments.created")
                .description("Total tournaments created")
                .register(registry);

        // WebSocket gauge
        registry.gauge("app.websocket.active_connections", activeWebSocketConnections);

        // Processing time
        this.bookingProcessingTime = Timer.builder("app.bookings.processing_time")
                .description("Time to process a booking request")
                .register(registry);
    }

    // ─── Booking Events ───

    public void recordBookingCreated(int tournamentId, int slotCount) {
        bookingsCreated.increment(slotCount);
        log.debug("Metric: booking created — tournament={}, slots={}", tournamentId, slotCount);
    }

    public void recordBookingFailed(int tournamentId, String reason) {
        bookingsFailed.increment();
        Counter.builder("app.bookings.failed.by_reason")
                .tag("reason", sanitizeTag(reason))
                .register(registry)
                .increment();
        log.debug("Metric: booking failed — tournament={}, reason={}", tournamentId, reason);
    }

    public void recordBookingCancelled(int slotId) {
        bookingsCancelled.increment();
        log.debug("Metric: booking cancelled — slot={}", slotId);
    }

    public Timer.Sample startBookingTimer() {
        return Timer.start(registry);
    }

    public void stopBookingTimer(Timer.Sample sample) {
        sample.stop(bookingProcessingTime);
    }

    // ─── Transaction Events ───

    public void recordDeposit(double amount) {
        depositsSubmitted.increment();
        log.debug("Metric: deposit submitted — amount={}", amount);
    }

    public void recordWithdrawal(double amount) {
        withdrawalsSubmitted.increment();
        log.debug("Metric: withdrawal submitted — amount={}", amount);
    }

    // ─── Engagement Events ───

    public void recordTournamentViewed(int tournamentId) {
        tournamentsViewed.increment();
    }

    public void recordSignInSuccess() {
        signInsSuccessful.increment();
    }

    public void recordSignInFailed(String reason) {
        signInsFailed.increment();
        Counter.builder("app.auth.signin.failed.by_reason")
                .tag("reason", sanitizeTag(reason))
                .register(registry)
                .increment();
    }

    // ─── WebSocket Events ───

    public void recordWebSocketConnected() {
        activeWebSocketConnections.incrementAndGet();
    }

    public void recordWebSocketDisconnected() {
        activeWebSocketConnections.updateAndGet(current -> Math.max(0, current - 1));
    }

    // ─── API Error Tracking ───

    public void recordApiError(String endpoint, int statusCode, String errorType) {
        Counter.builder("app.api.errors")
                .tag("endpoint", sanitizeTag(endpoint))
                .tag("status", String.valueOf(statusCode))
                .tag("type", sanitizeTag(errorType))
                .register(registry)
                .increment();
    }

    // ─── User Events ───

    public void recordUserRegistration(String method) {
        userRegistrations.increment();
        Counter.builder("app.users.registered.by_method")
                .tag("method", sanitizeTag(method))
                .register(registry)
                .increment();
    }

    // ─── Wallet Events ───

    public void recordWalletCredit(int amount, String reason) {
        walletCredits.increment();
        Counter.builder("app.wallet.credits.by_reason")
                .tag("reason", sanitizeTag(reason))
                .register(registry)
                .increment();
    }

    public void recordWalletDebit(int amount, String reason) {
        walletDebits.increment();
        Counter.builder("app.wallet.debits.by_reason")
                .tag("reason", sanitizeTag(reason))
                .register(registry)
                .increment();
    }

    // ─── Notification Events ───

    public void recordNotificationSent(String type) {
        notificationsSent.increment();
        Counter.builder("app.notifications.sent.by_type")
                .tag("type", sanitizeTag(type))
                .register(registry)
                .increment();
    }

    // ─── Tournament Management ───

    public void recordTournamentCreated() {
        tournamentsCreated.increment();
    }

    // ─── Admin Action Tracking ───

    public void recordAdminAction(String action, String adminUid) {
        Counter.builder("app.admin.actions")
                .tag("action", sanitizeTag(action))
                .register(registry)
                .increment();
        log.debug("Metric: admin action — action={}, admin={}", action, adminUid);
    }

    // ─── Helpers ───

    /**
     * Sanitize tag values for Prometheus compatibility (no special chars).
     */
    private String sanitizeTag(String value) {
        if (value == null) return "unknown";
        return value.replaceAll("[^a-zA-Z0-9_\\-.]", "_")
                .substring(0, Math.min(value.length(), 50));
    }
}
