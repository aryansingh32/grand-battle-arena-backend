package com.esport.EsportTournament.dto;

import lombok.Data;

@Data
public class UserStatsDTO {
    private long totalUsers;
    private long activeUsers;
    private long adminUsers;
    private long bannedUsers;
    private long inactiveUsers;
}
