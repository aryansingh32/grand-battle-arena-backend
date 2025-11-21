package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.dto.*;
import com.esport.EsportTournament.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Configure this properly for production
public class PaymentController {

    private final PaymentService paymentService;

    // ============ USER ENDPOINTS ============

    /**
     * Get QR code by amount (Public endpoint for users)
     * GET /api/v1/payments/qr/{amount}
     */
    @GetMapping("/qr/{amount}")
    public ResponseEntity<PaymentResponseDTO> getQrCodeByAmount(@PathVariable Integer amount) {
        log.info("User requesting QR code for amount: {}", amount);

        PaymentResponseDTO response = paymentService.getQrCodeByAmount(amount);
        return ResponseEntity.ok(response);
    }

    /**
     * Get QR code by amount via POST (Alternative for users)
     * POST /api/v1/payments/qr
     */
    @PostMapping("/qr")
    public ResponseEntity<PaymentResponseDTO> getQrCode(@Valid @RequestBody PaymentQueryDTO queryDTO) {
        log.info("User requesting QR code for amount: {}", queryDTO.getAmount());

        PaymentResponseDTO response = paymentService.getQrCodeByAmount(queryDTO.getAmount());
        return ResponseEntity.ok(response);
    }

    /**
     * Get all available amounts (Public endpoint for users)
     * GET /api/v1/payments/amounts
     */
    @GetMapping("/amounts")
    public ResponseEntity<List<AvailableAmountDTO>> getAvailableAmounts() { // Changed return type
        log.info("User requesting available payment amounts");

        List<AvailableAmountDTO> amountsWithOptions = paymentService.getAvailableAmounts(); // The service now returns the DTO list
        return ResponseEntity.ok(amountsWithOptions);
    }
    // ============ ADMIN ENDPOINTS ============

    /**
     * Create new QR code (Admin only)
     * POST /api/v1/payments/admin/create
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @PostMapping("/admin/create")
    public ResponseEntity<AdminPaymentResponseDTO> createPaymentQr(
            @Valid @RequestBody PaymentRequestDTO requestDTO,
            @RequestHeader(value = "X-Admin-User", defaultValue = "ADMIN") String adminUser) {

        log.info("Admin creating QR code for amount: {}", requestDTO.getAmount());

        AdminPaymentResponseDTO response = paymentService.createPaymentQr(requestDTO, adminUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Update existing QR code (Admin only)
     * PUT /api/v1/payments/admin/{amount}
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @PutMapping("/admin/{amount}")
    public ResponseEntity<AdminPaymentResponseDTO> updatePaymentQr(
            @PathVariable Integer amount,
            @Valid @RequestBody PaymentRequestDTO requestDTO,
            @RequestHeader(value = "X-Admin-User", defaultValue = "ADMIN") String adminUser) {

        log.info("Admin updating QR code for amount: {}", amount);

        AdminPaymentResponseDTO response = paymentService.updatePaymentQr(amount, requestDTO, adminUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Toggle QR code status (Admin only)
     * PATCH /api/v1/payments/admin/{amount}/toggle
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @PatchMapping("/admin/{amount}/toggle")
    public ResponseEntity<AdminPaymentResponseDTO> toggleQrStatus(
            @PathVariable Integer amount,
            @Valid @RequestBody AdminPasswordDTO passwordDTO,
            @RequestHeader(value = "X-Admin-User", defaultValue = "ADMIN") String adminUser) {

        log.info("Admin toggling status for QR code amount: {}", amount);

        AdminPaymentResponseDTO response = paymentService.toggleQrStatus(amount, passwordDTO, adminUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete QR code (Admin only)
     * DELETE /api/v1/payments/admin/{amount}
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @DeleteMapping("/admin/{amount}")
    public ResponseEntity<Map<String, String>> deletePaymentQr(
            @PathVariable Integer amount,
            @Valid @RequestBody AdminPasswordDTO passwordDTO) {

        log.info("Admin deleting QR code for amount: {}", amount);

        paymentService.deletePaymentQr(amount, passwordDTO);

        Map<String, String> response = Map.of(
                "message", "QR code deleted successfully",
                "amount", "₹" + amount
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get all QR codes (Admin only)
     * POST /api/v1/payments/admin/all
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_WALLET')")
    @PostMapping("/admin/all")
    public ResponseEntity<List<AdminPaymentResponseDTO>> getAllPaymentQrs(
            @Valid @RequestBody AdminPasswordDTO passwordDTO) {

        log.info("Admin requesting all QR codes");

        List<AdminPaymentResponseDTO> response = paymentService.getAllPaymentQrs(passwordDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     * GET /api/v1/payments/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = Map.of(
                "status", "UP",
                "service", "Payment QR Service",
                "timestamp", java.time.LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(response);
    }
}