package com.esport.EsportTournament.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SystemSettingsDTO {
    private MaintenanceSettings maintenance;
    private FeatureSettings features;
    private LimitSettings limits;
    private SecuritySettings security;

    @Data
    public static class MaintenanceSettings {
        private boolean enabled;
        private String message;
        private LocalDateTime scheduledStart;
        private int estimatedDuration;
    }

    @Data
    public static class FeatureSettings {
        private boolean userRegistration;
        private boolean tournamentBooking;
        private boolean walletTransactions;
        private boolean notifications;
    }

    @Data
    public static class LimitSettings {
        private int maxCoinBalance;
        private int maxTransactionAmount;
        private int maxSlotsPerUser;
        private int maxTournamentParticipants;
    }

    @Data
    public static class SecuritySettings {
        private boolean requireEmailVerification;
        private boolean enableTwoFactorAuth;
        private int maxLoginAttempts;
        private int sessionTimeoutMinutes;
    }
}
