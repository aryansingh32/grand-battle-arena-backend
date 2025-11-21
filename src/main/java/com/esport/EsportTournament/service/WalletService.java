package com.esport.EsportTournament.service;

import com.esport.EsportTournament.dto.WalletDTO;
import com.esport.EsportTournament.dto.WalletLedgerDTO;
import com.esport.EsportTournament.exception.ResourceNotFoundException;
import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.model.Wallet;
import com.esport.EsportTournament.model.WalletLedger;
import com.esport.EsportTournament.repository.UsersRepo;
import com.esport.EsportTournament.repository.WalletRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ✅ FIXED: Wallet Service with Proper Locking
 * - SERIALIZABLE isolation for all balance operations
 * - Prevents race conditions
 * - Integrated audit logging
 * - Proper validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepo walletRepo;
    private final UsersRepo usersRepo;
    private final AuditLogService auditLogService;
    private final EnhancedNotificationService notificationService;
    private final WalletLedgerService walletLedgerService;

    /**
     * ✅ FIXED: Create wallet with proper initialization
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public WalletDTO createWalletForUser(String firebaseUID) {
        log.info("💰 Creating wallet for user: {}", firebaseUID);

        // Check if wallet already exists
        if (walletRepo.findByUserId_FirebaseUserUID(firebaseUID).isPresent()) {
            throw new IllegalStateException("Wallet already exists for user: " + firebaseUID);
        }

        Users user = usersRepo.findByFirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + firebaseUID));

        Wallet wallet = new Wallet();
        wallet.setUserId(user);
        wallet.setCoins(0); // Initial balance
        wallet.setLastUpdated(LocalDateTime.now());

        Wallet savedWallet = walletRepo.save(wallet);

        auditLogService.logWalletOperation("WALLET_CREATED", firebaseUID, 0, 0);

        log.info("✅ Wallet created: user={}, balance=0", firebaseUID);
        return mapToDTO(savedWallet);
    }

    /**
     * ✅ FIXED: Add coins with proper locking
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public WalletDTO addCoins(String firebaseUID, int amount) {
        log.info("💰 Adding {} coins to user: {}", amount, firebaseUID);

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Wallet wallet = walletRepo.findByUserId_FirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + firebaseUID));

        int oldBalance = wallet.getCoins();
        wallet.setCoins(oldBalance + amount);
        wallet.setLastUpdated(LocalDateTime.now());

        Wallet updatedWallet = walletRepo.save(wallet);

        auditLogService.logWalletOperation("COINS_ADDED", firebaseUID, amount, updatedWallet.getCoins());
        walletLedgerService.recordEntry(updatedWallet, WalletLedger.Direction.CREDIT, amount,
                updatedWallet.getCoins(), "ADMIN_ADJUSTMENT", null, null, currentActor());
        notificationService.notifyDepositSuccess(firebaseUID, amount, updatedWallet.getCoins());

        log.info("✅ Coins added: user={}, added={}, newBalance={}", firebaseUID, amount, updatedWallet.getCoins());
        return mapToDTO(updatedWallet);
    }

    /**
     * ✅ FIXED: Deduct coins with proper validation
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public WalletDTO deductCoins(String firebaseUID, int amount) {
        log.info("💸 Deducting {} coins from user: {}", amount, firebaseUID);

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Wallet wallet = walletRepo.findByUserId_FirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + firebaseUID));

        if (wallet.getCoins() < amount) {
            throw new IllegalStateException(
                    String.format("Insufficient balance. Available: ₹%d, Required: ₹%d",
                            wallet.getCoins(), amount));
        }

        int oldBalance = wallet.getCoins();
        wallet.setCoins(oldBalance - amount);
        wallet.setLastUpdated(LocalDateTime.now());

        Wallet updatedWallet = walletRepo.save(wallet);

        auditLogService.logWalletOperation("COINS_DEDUCTED", firebaseUID, amount, updatedWallet.getCoins());
        walletLedgerService.recordEntry(updatedWallet, WalletLedger.Direction.DEBIT, amount,
                updatedWallet.getCoins(), "ADMIN_ADJUSTMENT", null, null, currentActor());

        log.info("✅ Coins deducted: user={}, deducted={}, newBalance={}", firebaseUID, amount, updatedWallet.getCoins());
        return mapToDTO(updatedWallet);
    }

    /**
     * ✅ NEW: Set wallet balance (Admin only)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public WalletDTO setWalletBalance(String firebaseUID, int newBalance, String adminUID) {
        log.info("🔧 Admin {} setting wallet balance for user {} to {}", adminUID, firebaseUID, newBalance);

        if (newBalance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }

        Wallet wallet = walletRepo.findByUserId_FirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + firebaseUID));

        int oldBalance = wallet.getCoins();
        wallet.setCoins(newBalance);
        wallet.setLastUpdated(LocalDateTime.now());

        Wallet updatedWallet = walletRepo.save(wallet);

        auditLogService.logWalletOperation("BALANCE_SET_BY_ADMIN", adminUID, newBalance, newBalance);

        log.info("✅ Balance set by admin: user={}, oldBalance={}, newBalance={}",
                firebaseUID, oldBalance, newBalance);
        int delta = newBalance - oldBalance;
        if (delta != 0) {
            walletLedgerService.recordEntry(updatedWallet,
                    delta > 0 ? WalletLedger.Direction.CREDIT : WalletLedger.Direction.DEBIT,
                    Math.abs(delta),
                    updatedWallet.getCoins(),
                    "ADMIN_SET_BALANCE",
                    null,
                    null,
                    adminUID);
        }
        return mapToDTO(updatedWallet);
    }

    /**
     * ✅ NEW: Transfer coins between users (Admin only)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void transferCoins(String fromUID, String toUID, int amount, String adminUID) {
        log.info("💸 Admin {} transferring {} coins from {} to {}", adminUID, amount, fromUID, toUID);

        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        if (fromUID.equals(toUID)) {
            throw new IllegalArgumentException("Cannot transfer to same user");
        }

        // Deduct from sender
        Wallet fromWallet = walletRepo.findByUserId_FirebaseUserUID(fromUID)
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found: " + fromUID));

        if (fromWallet.getCoins() < amount) {
            throw new IllegalStateException("Insufficient balance for transfer");
        }

        fromWallet.setCoins(fromWallet.getCoins() - amount);
        fromWallet.setLastUpdated(LocalDateTime.now());
        walletRepo.save(fromWallet);
        walletLedgerService.recordEntry(fromWallet, WalletLedger.Direction.DEBIT, amount,
                fromWallet.getCoins(), "TRANSFER", toUID, null, adminUID);

        // Add to receiver
        Wallet toWallet = walletRepo.findByUserId_FirebaseUserUID(toUID)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found: " + toUID));

        toWallet.setCoins(toWallet.getCoins() + amount);
        toWallet.setLastUpdated(LocalDateTime.now());
        walletRepo.save(toWallet);
        walletLedgerService.recordEntry(toWallet, WalletLedger.Direction.CREDIT, amount,
                toWallet.getCoins(), "TRANSFER", fromUID, null, adminUID);

        auditLogService.logWalletOperation("TRANSFER_ADMIN", adminUID, amount, amount);

        log.info("✅ Transfer completed: from={}, to={}, amount={}", fromUID, toUID, amount);
    }

    /**
     * Get wallet by Firebase UID
     */
    @Transactional(readOnly = true)
    public WalletDTO getWalletByFirebaseUID(String firebaseUID) {
        Wallet wallet = walletRepo.findByUserId_FirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + firebaseUID));
        return mapToDTO(wallet);
    }

    @Transactional(readOnly = true)
    public List<WalletLedgerDTO> getWalletLedger(String firebaseUID) {
        return walletLedgerService.getLedgerForUser(firebaseUID);
    }

    /**
     * Get all wallets (Admin only)
     */
    @Transactional(readOnly = true)
    public List<WalletDTO> getAllWallets() {
        return walletRepo.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ NEW: Get wallet statistics (Admin only)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWalletStatistics() {
        List<Wallet> allWallets = walletRepo.findAll();

        long totalWallets = allWallets.size();
        long totalCoins = allWallets.stream()
                .mapToLong(Wallet::getCoins)
                .sum();
        long walletsWithBalance = allWallets.stream()
                .filter(w -> w.getCoins() > 0)
                .count();
        double averageBalance = totalWallets > 0 ? (double) totalCoins / totalWallets : 0;

        return Map.of(
                "totalWallets", totalWallets,
                "totalCoins", totalCoins,
                "walletsWithBalance", walletsWithBalance,
                "averageBalance", Math.round(averageBalance),
                "emptyWallets", totalWallets - walletsWithBalance
        );
    }

    /**
     * Map entity to DTO
     */
    private WalletDTO mapToDTO(Wallet wallet) {
        return new WalletDTO(
                wallet.getId(),
                wallet.getUserId().getFirebaseUserUID(),
                wallet.getCoins(),
                wallet.getLastUpdated()
        );
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String principal) {
            return principal;
        }
        return "SYSTEM";
    }
}