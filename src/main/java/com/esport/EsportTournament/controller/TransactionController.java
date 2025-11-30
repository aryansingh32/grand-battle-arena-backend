package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.dto.CustomRequestDTO;
import com.esport.EsportTournament.dto.DepositRequestDTO;
import com.esport.EsportTournament.dto.TransactionTableDTO;
import com.esport.EsportTournament.dto.WithdrawRequestDTO;
import com.esport.EsportTournament.model.TransactionTable;
import com.esport.EsportTournament.service.TransactionTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionTableService transactionService;

    /**
     * Create deposit request
     * POST /api/transactions/deposit
     */
    @PostMapping("/deposit")
    public ResponseEntity<TransactionTableDTO> createDepositRequest(
            @Valid @RequestBody DepositRequestDTO depositRequestDTO,
            Authentication authentication) {

        String firebaseUID = getAuthenticatedUserUID(authentication);
        log.info("Creating deposit request for user: {} amount: {}", firebaseUID, depositRequestDTO.getAmount());

        TransactionTableDTO transaction = transactionService.createDepositRequest(
                firebaseUID,
                depositRequestDTO.getTransactionUID(),
                depositRequestDTO.getAmount()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    /**
     * Create withdrawal request
     * POST /api/transactions/withdraw
     */
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionTableDTO> createWithdrawalRequest(
            @RequestBody WithdrawRequestDTO request,
            Authentication authentication) {

        Integer amount = request.getAmount();
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        String firebaseUID = getAuthenticatedUserUID(authentication);
        log.info("Creating withdrawal request for user: {} amount: {}", firebaseUID, amount);

        TransactionTableDTO transaction = transactionService.createWithdrawalRequest(
                firebaseUID, 
                amount, 
                request.getTransactionUID() // Pass UPI ID
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    /**
     * Admin approves transaction
     * PUT /api/transactions/{transactionId}/approve
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TRANSACTIONS')")
    @PutMapping("/{transactionId}/approve")
    public ResponseEntity<TransactionTableDTO> approveTransaction(
            @PathVariable String transactionId,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        log.info("Admin {} approving transaction: {}", adminUID, transactionId);

        TransactionTableDTO approvedTransaction = transactionService.approveTransaction(transactionId, adminUID);
        return ResponseEntity.ok(approvedTransaction);
    }

    /**
     * Admin rejects transaction
     * PUT /api/transactions/{transactionId}/reject
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TRANSACTIONS')")
    @PutMapping("/{transactionId}/reject")
    public ResponseEntity<TransactionTableDTO> rejectTransaction(
            @PathVariable String transactionId,
            Authentication authentication) {

        String adminUID = getAuthenticatedUserUID(authentication);
        log.info("Admin {} rejecting transaction: {}", adminUID, transactionId);

        TransactionTableDTO rejectedTransaction = transactionService.rejectTransaction(transactionId, adminUID);
        return ResponseEntity.ok(rejectedTransaction);
    }



    /**
     * Get user's transaction history
     * GET /api/transactions/history
     */
    @GetMapping("/history")
    public ResponseEntity<List<TransactionTableDTO>> getUserTransactionHistory(Authentication authentication) {
        String firebaseUID = getAuthenticatedUserUID(authentication);
        log.debug("Fetching transaction history for user: {}", firebaseUID);

        List<TransactionTableDTO> transactions = transactionService.getUserTransactions(firebaseUID);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Admin makes custom transaction (add/deduct coins directly)
     * POST /api/transactions/custom/{firebaseUID}
     */


    //NOTE use Wallet to add or remove coins custom on anyones account
//    @PreAuthorize("hasRole('ADMIN')")
//    @PostMapping("/custom/{firebaseUID}")
//    public ResponseEntity<Map<String, String>> makeCustomTransaction(
//            @PathVariable String firebaseUID,
//            @Valid @RequestBody CustomRequestDTO customRequestDTO,
//            Authentication authentication) {
//
//        String adminUID = getAuthenticatedUserUID(authentication);
//        log.info("Admin {} making custom transaction for user {}: type={}, amount={}",
//                adminUID, firebaseUID, customRequestDTO.getType(), customRequestDTO.getAmount());
//
//        Map<String, String> response = transactionService.makeCustomTransaction(
//                firebaseUID,
//                customRequestDTO.getType(),
//                customRequestDTO.getAmount(),
//                adminUID
//        );
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }

    /**
     * Utility: extract Firebase UID from Authentication
     */
    private String getAuthenticatedUserUID(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return authentication.getName(); // usually Firebase UID is set as principal
    }

    // ADD TO TransactionController.java
    @PreAuthorize("hasAuthority('PERM_MANAGE_TRANSACTIONS')")
    @GetMapping
    public ResponseEntity<List<TransactionTableDTO>> getAllTransactions() {
        List<TransactionTableDTO> transactions = transactionService.getAllTransactionTable();
        return ResponseEntity.ok(transactions);
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_TRANSACTIONS')")
    @GetMapping("/pending")
    public ResponseEntity<List<TransactionTableDTO>> getPendingTransactions() {
        List<TransactionTableDTO> transactions = transactionService.getPendingTransactions();
        return ResponseEntity.ok(transactions);
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_TRANSACTIONS')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTransactionStats() {
        Map<String, Object> stats = transactionService.getTransactionStats();
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_TRANSACTIONS')")
    @GetMapping("/{id}")
    public ResponseEntity<TransactionTableDTO> getTransactionById(@PathVariable String id) {
        TransactionTableDTO transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(transaction);
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelTransaction(
            @PathVariable String id,
            Authentication authentication) {

        String firebaseUID = (String) authentication.getPrincipal();
        transactionService.cancelPendingTransaction(id, firebaseUID);

        return ResponseEntity.ok(Map.of("message", "Transaction cancelled successfully"));
    }


}
