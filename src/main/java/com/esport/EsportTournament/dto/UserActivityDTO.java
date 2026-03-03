package com.esport.EsportTournament.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserActivityDTO {
    private UserDTO user;
    private WalletDTO wallet;
    private int transactionCount;
    private int bookedSlotsCount;
    private List<TransactionTableDTO> recentTransactions;
    private List<SlotsDTO> recentBookings;
}
