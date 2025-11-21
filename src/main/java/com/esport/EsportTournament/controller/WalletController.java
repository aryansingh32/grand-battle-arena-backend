package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.dto.WalletDTO;
import com.esport.EsportTournament.dto.WalletLedgerDTO;
import com.esport.EsportTournament.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @GetMapping
    public List<WalletDTO> getAllWallet() {
        return walletService.getAllWallets();
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @PostMapping("/{firebaseUID}")
    public ResponseEntity<WalletDTO> createWallet(@PathVariable String firebaseUID) {
        return ResponseEntity.status(201).body(walletService.createWalletForUser(firebaseUID));
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET') or #firebaseUID == authentication.name")
    @GetMapping("/{firebaseUID}")
    public ResponseEntity<WalletDTO> getWallet(@PathVariable String firebaseUID) {
        return ResponseEntity.ok(walletService.getWalletByFirebaseUID(firebaseUID));
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET') or #firebaseUID == authentication.name")
    @GetMapping("/{firebaseUID}/ledger")
    public ResponseEntity<List<WalletLedgerDTO>> getWalletLedger(
            @PathVariable String firebaseUID) {
        return ResponseEntity.ok(walletService.getWalletLedger(firebaseUID));
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @PostMapping("/{firebaseUID}/add")
    public ResponseEntity<WalletDTO> addCoins(
            @PathVariable String firebaseUID,
            @RequestBody Map<String, Integer> requestBody
    ) {
        int amount = requestBody.get("amount");
        return ResponseEntity.ok(walletService.addCoins(firebaseUID, amount));
    }
    // ADD TO WalletController.java
    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getWalletStats() {
        Map<String, Object> stats = walletService.getWalletStatistics();
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, String>> transferCoins(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = (String) authentication.getPrincipal();
        String fromUID = (String) request.get("fromUID");
        String toUID = (String) request.get("toUID");
        Integer amount = (Integer) request.get("amount");

        walletService.transferCoins(fromUID, toUID, amount, adminUID);

        return ResponseEntity.ok(Map.of("message", "Transfer completed successfully"));
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @PutMapping("/{firebaseUID}/balance")
    public ResponseEntity<WalletDTO> setWalletBalance(
            @PathVariable String firebaseUID,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String adminUID = (String) authentication.getPrincipal();
        Integer newBalance = (Integer) request.get("balance");

        WalletDTO wallet = walletService.setWalletBalance(firebaseUID, newBalance, adminUID);
        return ResponseEntity.ok(wallet);
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @PostMapping("/{firebaseUID}/deduct")
    public ResponseEntity<WalletDTO> deductCoins(
            @PathVariable String firebaseUID,
            @RequestBody Map<String, Integer> requestBody
    ) {
        int amount = requestBody.get("amount");
        return ResponseEntity.ok(walletService.deductCoins(firebaseUID, amount));
    }
}
