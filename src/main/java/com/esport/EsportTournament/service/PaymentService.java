package com.esport.EsportTournament.service;

import com.esport.EsportTournament.dto.*;
import com.esport.EsportTournament.exception.DuplicateAmountException;
import com.esport.EsportTournament.exception.PaymentNotFoundException;
import com.esport.EsportTournament.model.Payment;
import com.esport.EsportTournament.repository.PaymentRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Payment QR management service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepo paymentRepo;
    



    /**
     * Get QR code by amount (Public endpoint)
     */
    @Transactional(readOnly = true)
    public PaymentResponseDTO getQrCodeByAmount(Integer amount) {
        log.info("🔍 Fetching QR code for amount: ₹{}", amount);

        Payment payment = paymentRepo.findByAmountAndIsActiveTrue(amount)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No active QR code found for amount: ₹" + amount));

        return new PaymentResponseDTO(
                payment.getId(),
                payment.getAmount(),
                payment.getUpiIdQrLink(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getIsActive()
        );
    }

    /**
     * Get all available amounts (Public endpoint)
     */
    @Transactional(readOnly = true)
    public List<AvailableAmountDTO> getAvailableAmounts() {
        log.info("🔍 Fetching all available payment amounts");

        return paymentRepo.findByIsActiveTrueOrderByAmountAsc().stream()
                .map(p -> new AvailableAmountDTO(p.getAmount(), p.getCoins()))
                .collect(Collectors.toList());
    }

    /**
     * ✅ FIXED: Create payment QR with secure authentication
     */
    @Transactional
    public AdminPaymentResponseDTO createPaymentQr(PaymentRequestDTO requestDTO, String adminUser) {
        log.info("➕ Admin creating QR code for amount: ₹{}", requestDTO.getAmount());

        // Check for duplicate amount
        if (paymentRepo.existsByAmount(requestDTO.getAmount())) {
            throw new DuplicateAmountException(
                    "QR code for amount ₹" + requestDTO.getAmount() + " already exists");
        }

        Payment payment = new Payment();
        payment.setAmount(requestDTO.getAmount());
        payment.setCoins(requestDTO.getCoin());
        payment.setUpiIdQrLink(requestDTO.getUpiIdQrLink());
        payment.setAddedBy(adminUser);
        payment.setIsActive(true);

        Payment savedPayment = paymentRepo.save(payment);

        log.info("✅ QR code created successfully: ID={}", savedPayment.getId());
        return mapToAdminResponse(savedPayment);
    }

    /**
     * ✅ FIXED: Update payment QR with secure authentication
     */
    @Transactional
    public AdminPaymentResponseDTO updatePaymentQr(Integer amount, PaymentRequestDTO requestDTO, String adminUser) {
        log.info("🔄 Admin updating QR code for amount: ₹{}", amount);

        Payment payment = paymentRepo.findByAmount(amount)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "QR code not found for amount: ₹" + amount));

        // Check if new amount conflicts with existing
        if (!amount.equals(requestDTO.getAmount()) &&
                paymentRepo.existsByAmountAndNotId(requestDTO.getAmount(), payment.getId())) {
            throw new DuplicateAmountException(
                    "QR code for amount ₹" + requestDTO.getAmount() + " already exists");
        }

        payment.setAmount(requestDTO.getAmount());
        payment.setCoins(requestDTO.getCoin());
        payment.setUpiIdQrLink(requestDTO.getUpiIdQrLink());
        payment.setModifiedBy(adminUser);

        Payment updatedPayment = paymentRepo.save(payment);

        log.info("✅ QR code updated successfully: ID={}", updatedPayment.getId());
        return mapToAdminResponse(updatedPayment);
    }

    /**
     * ✅ FIXED: Toggle QR status with secure authentication
     */
    @Transactional
    public AdminPaymentResponseDTO toggleQrStatus(Integer amount, String adminUser) {
        log.info("🔄 Admin toggling status for amount: ₹{}", amount);

        Payment payment = paymentRepo.findByAmount(amount)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "QR code not found for amount: ₹" + amount));

        payment.setIsActive(!payment.getIsActive());
        payment.setModifiedBy(adminUser);

        Payment updatedPayment = paymentRepo.save(payment);

        log.info("✅ QR status toggled: ID={}, Active={}", updatedPayment.getId(), updatedPayment.getIsActive());
        return mapToAdminResponse(updatedPayment);
    }

    /**
     * ✅ FIXED: Delete payment QR with secure authentication
     */
    @Transactional
    public void deletePaymentQr(Integer amount) {
        log.info("🗑️ Admin deleting QR code for amount: ₹{}", amount);

        Payment payment = paymentRepo.findByAmount(amount)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "QR code not found for amount: ₹" + amount));

        paymentRepo.delete(payment);

        log.info("✅ QR code deleted successfully: ₹{}", amount);
    }

    /**
     * ✅ FIXED: Get all QR codes with secure authentication
     */
    @Transactional(readOnly = true)
    public List<AdminPaymentResponseDTO> getAllPaymentQrs() {
        log.info("🔍 Admin fetching all QR codes");

        return paymentRepo.findAllByOrderByAmountAsc().stream()
                .map(this::mapToAdminResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map entity to admin response DTO
     */
    private AdminPaymentResponseDTO mapToAdminResponse(Payment payment) {
        return new AdminPaymentResponseDTO(
                payment.getId(),
                payment.getAmount(),
                payment.getUpiIdQrLink(),
                payment.getAddedBy(),
                payment.getModifiedBy(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getIsActive()
        );
    }
}
