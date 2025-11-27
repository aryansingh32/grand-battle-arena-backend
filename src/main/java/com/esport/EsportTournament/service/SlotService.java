package com.esport.EsportTournament.service;

import com.esport.EsportTournament.dto.SlotsDTO;
import com.esport.EsportTournament.dto.TeamBookingRequestDTO;
import com.esport.EsportTournament.exception.ResourceNotFoundException;
import com.esport.EsportTournament.model.Slots;
import com.esport.EsportTournament.model.Tournaments;
import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.model.Wallet;
import com.esport.EsportTournament.model.WalletLedger;
import com.esport.EsportTournament.repository.SlotRepo;
import com.esport.EsportTournament.repository.TournamentRepo;
import com.esport.EsportTournament.repository.UsersRepo;
import com.esport.EsportTournament.repository.WalletRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ FIXED: Slot Service with Proper Concurrency Control
 * - Added SERIALIZABLE isolation for bookings
 * - Fixed race conditions with pessimistic locking
 * - Added team booking support
 * - Integrated notification service
 * - Added proper validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlotService {

        private final SlotRepo slotRepo;
        private final TournamentRepo tournamentRepo;
        private final UsersRepo usersRepo;
        private final WalletRepo walletRepo;
        private final EnhancedNotificationService notificationService;
        private final AuditLogService auditLogService;
        private final WalletLedgerService walletLedgerService;

        /**
         * ✅ FIXED: Book specific slot with proper locking and validation
         */
        @Transactional(isolation = Isolation.SERIALIZABLE)
        public SlotsDTO bookSpecificSlot(int tournamentId, String firebaseUID, String playerName, int slotNumber) {
                log.info("🎯 User {} attempting to book slot {} for tournament {}", firebaseUID, slotNumber,
                                tournamentId);

                // Validate inputs
                if (playerName == null || playerName.trim().isEmpty()) {
                        throw new IllegalArgumentException("Player name is required");
                }
                if (slotNumber < 1) {
                        throw new IllegalArgumentException("Invalid slot number");
                }

                // Get tournament
                Tournaments tournament = tournamentRepo.findById(tournamentId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Tournament not found: " + tournamentId));

                // Validate tournament status
                if (tournament.getStatus() != Tournaments.TournamentStatus.UPCOMING) {
                        throw new IllegalStateException(
                                        "Tournament is not accepting bookings: " + tournament.getStatus());
                }

                // Check if tournament has started
                if (tournament.getStartTime().isBefore(LocalDateTime.now())) {
                        throw new IllegalStateException("Tournament has already started");
                }

                // Get user
                Users user = usersRepo.findByFirebaseUserUID(firebaseUID)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + firebaseUID));

                // Check if user already booked a slot in this tournament
                if (slotRepo.existsByTournaments_IdAndUser_FirebaseUserUID(tournamentId, firebaseUID)) {
                        throw new IllegalStateException("You have already booked a slot in this tournament");
                }

                // Get slot with pessimistic lock (prevents race conditions)
                Slots slot = slotRepo.findByTournaments_IdAndSlotNumberForUpdate(tournamentId, slotNumber)
                                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotNumber));

                // Check if slot is available
                if (slot.getStatus() != Slots.SlotStatus.AVAILABLE) {
                        throw new IllegalStateException("Slot is already booked");
                }

                // Check wallet balance with pessimistic lock
                Wallet wallet = walletRepo.findByUserId_FirebaseUserUID(firebaseUID)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

                int entryFee = tournament.getEntryFees();
                if (wallet.getCoins() < entryFee) {
                        throw new IllegalStateException(
                                        String.format("Insufficient balance. Required: ₹%d, Available: ₹%d",
                                                        entryFee, wallet.getCoins()));
                }

                // Deduct entry fee
                wallet.setCoins(wallet.getCoins() - entryFee);
                wallet.setLastUpdated(LocalDateTime.now());
                walletRepo.save(wallet);
                walletLedgerService.recordEntry(wallet, WalletLedger.Direction.DEBIT, entryFee,
                                wallet.getCoins(), "TOURNAMENT_BOOK", String.valueOf(tournamentId), null, firebaseUID);

                // Book the slot
                slot.setUser(user);
                slot.setPlayerName(playerName);
                slot.setStatus(Slots.SlotStatus.BOOKED);
                slot.setBookedAt(LocalDateTime.now());
                Slots bookedSlot = slotRepo.save(slot);

                // Audit log
                auditLogService.logSlotBooking(firebaseUID, tournamentId, slotNumber, entryFee);

                // Notify user
                notificationService.notifySlotBooked(firebaseUID, tournamentId, tournament.getName(),
                                slotNumber, entryFee);

                log.info("✅ Slot booked successfully: user={}, tournament={}, slot={}",
                                firebaseUID, tournamentId, slotNumber);

                return mapToDTO(bookedSlot);
        }

        /**
         * ✅ NEW: Book team slots (for DUO, SQUAD, HEXA)
         */
        @Transactional(isolation = Isolation.SERIALIZABLE)
        public List<SlotsDTO> bookTeamSlots(int tournamentId, String firebaseUID,
                        List<TeamBookingRequestDTO.PlayerInfo> players) {
                log.info("🎯 User {} booking team slots for tournament {}", firebaseUID, tournamentId);

                // Validate
                if (players == null || players.isEmpty()) {
                        throw new IllegalArgumentException("Player list cannot be empty");
                }

                Tournaments tournament = tournamentRepo.findById(tournamentId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Tournament not found: " + tournamentId));

                // Validate team size
                int playersPerTeam = tournament.getPlayersPerTeam();
                if (players.size() != playersPerTeam) {
                        throw new IllegalArgumentException(
                                        String.format("This tournament requires exactly %d players per team",
                                                        playersPerTeam));
                }

                // Validate tournament status
                if (tournament.getStatus() != Tournaments.TournamentStatus.UPCOMING) {
                        throw new IllegalStateException("Tournament is not accepting bookings");
                }

                // Get user and wallet
                Users user = usersRepo.findByFirebaseUserUID(firebaseUID)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + firebaseUID));

                Wallet wallet = walletRepo.findByUserId_FirebaseUserUID(firebaseUID)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

                // Calculate total cost
                int totalCost = tournament.getEntryFees() * players.size();
                if (wallet.getCoins() < totalCost) {
                        throw new IllegalStateException(
                                        String.format("Insufficient balance. Required: ₹%d, Available: ₹%d",
                                                        totalCost, wallet.getCoins()));
                }

                // Book all slots
                List<SlotsDTO> bookedSlots = new ArrayList<>();
                for (TeamBookingRequestDTO.PlayerInfo player : players) {
                        Slots slot = slotRepo
                                        .findByTournaments_IdAndSlotNumberForUpdate(tournamentId,
                                                        player.getSlotNumber())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Slot not found: " + player.getSlotNumber()));

                        if (slot.getStatus() != Slots.SlotStatus.AVAILABLE) {
                                throw new IllegalStateException(
                                                "Slot " + player.getSlotNumber() + " is already booked");
                        }

                        slot.setUser(user);
                        slot.setPlayerName(player.getPlayerName());
                        slot.setStatus(Slots.SlotStatus.BOOKED);
                        slot.setBookedAt(LocalDateTime.now());
                        Slots bookedSlot = slotRepo.save(slot);
                        bookedSlots.add(mapToDTO(bookedSlot));
                }

                // Deduct total cost
                wallet.setCoins(wallet.getCoins() - totalCost);
                wallet.setLastUpdated(LocalDateTime.now());
                walletRepo.save(wallet);
                walletLedgerService.recordEntry(wallet, WalletLedger.Direction.DEBIT, totalCost,
                                wallet.getCoins(), "TEAM_TOURNAMENT_BOOK", String.valueOf(tournamentId), null,
                                firebaseUID);

                // Audit log
                auditLogService.logTeamBooking(firebaseUID, tournamentId, players.size(), totalCost);

                // Notify user
                notificationService.notifySlotBooked(firebaseUID, tournamentId, tournament.getName(),
                                players.get(0).getSlotNumber(), totalCost);

                log.info("✅ Team slots booked: user={}, tournament={}, slots={}",
                                firebaseUID, tournamentId, players.size());

                return bookedSlots;
        }

        /**
         * ✅ FIXED: Book next available slot
         */
        @Transactional(isolation = Isolation.SERIALIZABLE)
        public SlotsDTO bookNextAvailableSlot(int tournamentId, String firebaseUID, String playerName) {
                log.info("🎯 User {} booking next available slot for tournament {}", firebaseUID, tournamentId);

                // Find next available slot with pessimistic lock
                Slots slot = slotRepo.findFirstByTournaments_IdAndStatusOrderBySlotNumberAsc(
                                tournamentId, Slots.SlotStatus.AVAILABLE)
                                .orElseThrow(() -> new IllegalStateException("No available slots"));

                return bookSpecificSlot(tournamentId, firebaseUID, playerName, slot.getSlotNumber());
        }

        /**
         * ✅ FIXED: Cancel slot booking with refund
         */
        @Transactional(isolation = Isolation.SERIALIZABLE)
        public void cancelSlotBooking(int slotId, String firebaseUID) {
                log.info("🔄 User {} cancelling slot: {}", firebaseUID, slotId);

                Slots slot = slotRepo.findById(slotId)
                                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));

                // Verify ownership
                if (!slot.getUser().getFirebaseUserUID().equals(firebaseUID)) {
                        throw new IllegalStateException("You can only cancel your own bookings");
                }

                // Check if tournament has started
                if (slot.getTournaments().getStartTime().isBefore(LocalDateTime.now())) {
                        throw new IllegalStateException("Cannot cancel after tournament has started");
                }

                // Refund entry fee
                Wallet wallet = walletRepo.findByUserId_FirebaseUserUID(firebaseUID)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

                int refundAmount = slot.getTournaments().getEntryFees();
                wallet.setCoins(wallet.getCoins() + refundAmount);
                wallet.setLastUpdated(LocalDateTime.now());
                walletRepo.save(wallet);
                walletLedgerService.recordEntry(wallet, WalletLedger.Direction.CREDIT, refundAmount,
                                wallet.getCoins(), "TOURNAMENT_REFUND", String.valueOf(slot.getTournaments().getId()),
                                null, firebaseUID);

                // Release slot
                slot.setUser(null);
                slot.setPlayerName(null);
                slot.setStatus(Slots.SlotStatus.AVAILABLE);
                slot.setBookedAt(null);
                slotRepo.save(slot);

                // Audit log
                auditLogService.logSlotCancellation(firebaseUID, slot.getTournaments().getId(), slotId, refundAmount);

                // Notify user
                notificationService.notifyBookingCancelled(firebaseUID, slot.getTournaments().getId(),
                                slot.getTournaments().getName(), refundAmount);

                log.info("✅ Slot cancelled and refunded: slot={}, refund={}", slotId, refundAmount);
        }

        /**
         * ✅ NEW: Admin cancel slot
         */
        @Transactional(isolation = Isolation.SERIALIZABLE)
        public void adminCancelSlotBooking(int slotId, String adminUID) {
                log.info("🔄 Admin {} cancelling slot: {}", adminUID, slotId);

                Slots slot = slotRepo.findById(slotId)
                                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + slotId));

                if (slot.getStatus() != Slots.SlotStatus.BOOKED) {
                        throw new IllegalStateException("Slot is not booked");
                }

                String userUID = slot.getUser().getFirebaseUserUID();

                // Refund entry fee
                Wallet wallet = walletRepo.findByUserId_FirebaseUserUID(userUID)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

                int refundAmount = slot.getTournaments().getEntryFees();
                wallet.setCoins(wallet.getCoins() + refundAmount);
                wallet.setLastUpdated(LocalDateTime.now());
                walletRepo.save(wallet);

                // Release slot
                slot.setUser(null);
                slot.setPlayerName(null);
                slot.setStatus(Slots.SlotStatus.AVAILABLE);
                slot.setBookedAt(null);
                slotRepo.save(slot);

                // Audit log
                auditLogService.logAdminSlotCancellation(adminUID, slot.getTournaments().getId(), slotId, refundAmount);

                // Notify user
                notificationService.notifyBookingCancelled(userUID, slot.getTournaments().getId(),
                                slot.getTournaments().getName(), refundAmount);

                log.info("✅ Admin cancelled slot: slot={}, admin={}", slotId, adminUID);
        }

        /**
         * ✅ NEW: Pre-generate slots for tournament
         */
        @Transactional
        public void preGenerateSlots(int tournamentId, int maxPlayers) {
                log.info("🔨 Pre-generating {} slots for tournament {}", maxPlayers, tournamentId);

                Tournaments tournament = tournamentRepo.findById(tournamentId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Tournament not found: " + tournamentId));

                // Check for existing slots
                List<Slots> existingSlots = slotRepo.findByTournaments_Id(tournamentId);
                if (!existingSlots.isEmpty()) {
                        // 🔥 CRITICAL FIX: Check if any slots are booked before deleting
                        boolean hasBookings = existingSlots.stream()
                                        .anyMatch(s -> s.getStatus() == Slots.SlotStatus.BOOKED);

                        if (hasBookings) {
                                log.warn("⚠️ Cannot regenerate slots for tournament {}: Found existing bookings!",
                                                tournamentId);
                                throw new IllegalStateException(
                                                "Cannot regenerate slots: Tournament already has bookings. Please cancel bookings first.");
                        }

                        slotRepo.deleteAll(existingSlots);
                        log.info("Deleted {} existing slots", existingSlots.size());
                }

                // Generate new slots
                List<Slots> newSlots = new ArrayList<>();
                for (int i = 1; i <= maxPlayers; i++) {
                        Slots slot = new Slots();
                        slot.setTournaments(tournament);
                        slot.setSlotNumber(i);
                        slot.setStatus(Slots.SlotStatus.AVAILABLE);
                        newSlots.add(slot);
                }

                slotRepo.saveAll(newSlots);
                log.info("✅ Generated {} new slots for tournament {}", maxPlayers, tournamentId);
        }

        /**
         * Get slots for tournament
         */
        @Transactional(readOnly = true)
        public List<SlotsDTO> getSlots(int tournamentId) {
                return slotRepo.findByTournaments_Id(tournamentId).stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());
        }

        /**
         * Get user's booked slots
         */
        @Transactional(readOnly = true)
        public List<SlotsDTO> getUserBookedSlots(String firebaseUID) {
                return slotRepo.findByUser_FirebaseUserUID(firebaseUID).stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());
        }

        /**
         * ✅ FIXED: Get slot summary (optimized query)
         */
        @Transactional(readOnly = true)
        public Map<String, Object> getSlotSummary(int tournamentId) {
                List<Slots> allSlots = slotRepo.findByTournaments_Id(tournamentId);

                // Convert to DTOs - always return array, never null
                List<SlotsDTO> slotsList = allSlots.stream()
                                .map(this::mapToDTO)
                                .collect(Collectors.toList());

                long totalSlots = allSlots.size();
                long bookedSlots = allSlots.stream()
                                .filter(s -> s.getStatus() == Slots.SlotStatus.BOOKED)
                                .count();
                long availableSlots = allSlots.stream()
                                .filter(s -> s.getStatus() == Slots.SlotStatus.AVAILABLE)
                                .count();

                Map<String, Object> summary = new HashMap<>();
                summary.put("slots", slotsList); // Always return array, never null
                summary.put("totalSlots", totalSlots);
                summary.put("bookedCount", bookedSlots);
                summary.put("availableCount", availableSlots);
                summary.put("fillRate", totalSlots > 0 ? (bookedSlots * 100.0 / totalSlots) : 0);

                return summary;
        }

        /**
         * ✅ NEW: Process tournament cancellation (Bulk Refund)
         */
        @Transactional(isolation = Isolation.SERIALIZABLE)
        public void processTournamentCancellation(int tournamentId) {
                log.info("🚨 Processing cancellation for tournament: {}", tournamentId);

                Tournaments tournament = tournamentRepo.findById(tournamentId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Tournament not found: " + tournamentId));

                // Find all booked slots
                List<Slots> bookedSlots = slotRepo.findByTournaments_Id(tournamentId).stream()
                                .filter(s -> s.getStatus() == Slots.SlotStatus.BOOKED)
                                .collect(Collectors.toList());

                if (bookedSlots.isEmpty()) {
                        log.info("ℹ️ No bookings found for tournament {}. No refunds needed.", tournamentId);
                        return;
                }

                log.info("💰 Processing refunds for {} bookings...", bookedSlots.size());

                int refundAmount = tournament.getEntryFees();
                int successCount = 0;

                for (Slots slot : bookedSlots) {
                        try {
                                Users user = slot.getUser();
                                if (user == null)
                                        continue;

                                String firebaseUID = user.getFirebaseUserUID();

                                // Refund to wallet
                                Wallet wallet = walletRepo.findByUserId_FirebaseUserUID(firebaseUID)
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Wallet not found for user: " + firebaseUID));

                                wallet.setCoins(wallet.getCoins() + refundAmount);
                                wallet.setLastUpdated(LocalDateTime.now());
                                walletRepo.save(wallet);

                                // Record ledger
                                walletLedgerService.recordEntry(wallet, WalletLedger.Direction.CREDIT, refundAmount,
                                                wallet.getCoins(), "TOURNAMENT_CANCELLED_REFUND",
                                                String.valueOf(tournamentId), null, "SYSTEM");

                                // Reset slot
                                slot.setUser(null);
                                slot.setPlayerName(null);
                                slot.setStatus(Slots.SlotStatus.AVAILABLE);
                                slot.setBookedAt(null);
                                slotRepo.save(slot);

                                // Notify user
                                notificationService.notifyBookingCancelled(firebaseUID, tournamentId,
                                                tournament.getName(), refundAmount);

                                successCount++;
                        } catch (Exception e) {
                                log.error("❌ Failed to refund user {} for slot {}: {}",
                                                slot.getUser() != null ? slot.getUser().getFirebaseUserUID()
                                                                : "unknown",
                                                slot.getId(), e.getMessage());
                        }
                }

                log.info("✅ Cancellation processing complete. Refunded {}/{} users.", successCount, bookedSlots.size());
        }

        /**
         * Map entity to DTO
         */
        private SlotsDTO mapToDTO(Slots slot) {
                SlotsDTO dto = new SlotsDTO();
                dto.setId(slot.getId());
                dto.setTournamentId(slot.getTournaments().getId());
                dto.setSlotNumber(slot.getSlotNumber());
                dto.setFirebaseUserUID(slot.getUser() != null ? slot.getUser().getFirebaseUserUID() : null);
                dto.setPlayerName(slot.getPlayerName());
                dto.setStatus(slot.getStatus());
                dto.setBookedAt(slot.getBookedAt());
                return dto;
        }
}