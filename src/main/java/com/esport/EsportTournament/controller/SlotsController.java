package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.dto.SlotsDTO;
import com.esport.EsportTournament.dto.TeamBookingRequestDTO;
import com.esport.EsportTournament.service.SlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/slots")
@RequiredArgsConstructor
@Validated
public class SlotsController {

    private final SlotService slotsService;

    /**
     * Book specific slot for tournament
     * POST /api/slots/book
     */
    @PostMapping("/book")
    public ResponseEntity<SlotsDTO> bookSlot(
            @Valid @RequestBody SlotsDTO slotRequest,
            Authentication authentication) {

        String firebaseUID = getAuthenticatedUserUID(authentication);

        // Override the UID from token (security measure)
        slotRequest.setFirebaseUserUID(firebaseUID);

        log.info("User {} booking slot {} for tournament {}",
                firebaseUID, slotRequest.getSlotNumber(), slotRequest.getTournamentId());

        SlotsDTO bookedSlot = slotsService.bookSpecificSlot(
                slotRequest.getTournamentId(),
                firebaseUID,
                slotRequest.getPlayerName(),
                slotRequest.getSlotNumber()
        );

        return ResponseEntity.ok(bookedSlot);
    }

    @PostMapping("/book-team")
    public ResponseEntity<List<SlotsDTO>> bookTeamSlots(
        @Valid @RequestBody TeamBookingRequestDTO req,
        Authentication authentication) {

    String firebaseUID = getAuthenticatedUserUID(authentication);
    List<SlotsDTO> booked = slotsService.bookTeamSlots(
            req.getTournamentId(), firebaseUID, req.getPlayers());
    return ResponseEntity.ok(booked);
}


    /**
     * Book next available slot for tournament
     * POST /api/slots/book-next/{tournamentId}
     */
    @PostMapping("/book-next/{tournamentId}")
    public ResponseEntity<SlotsDTO> bookNextAvailableSlot(
            @PathVariable int tournamentId,
            @RequestBody Map<String, String> requestBody,
            Authentication authentication) {

        String firebaseUID = getAuthenticatedUserUID(authentication);
        String playerName = requestBody.get("playerName");

        if (playerName == null || playerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name is required");
        }

        log.info("User {} booking next available slot for tournament {}", firebaseUID, tournamentId);

        SlotsDTO bookedSlot = slotsService.bookNextAvailableSlot(
                tournamentId,
                firebaseUID,
                playerName
        );

        return ResponseEntity.ok(bookedSlot);
    }

    /**
     * Get slot summary for tournament
     * GET /api/slots/{tournamentId}/summary
     */
    @GetMapping("/{tournamentId}/summary")
    public ResponseEntity<Map<String, Object>> getSlotSummary(@PathVariable int tournamentId) {
        log.debug("Fetching slot summary for tournament: {}", tournamentId);

        Map<String, Object> summary = slotsService.getSlotSummary(tournamentId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get all slots for tournament
     * GET /api/slots/{tournamentId}
     */
    @GetMapping("/{tournamentId}")
    public ResponseEntity<List<SlotsDTO>> getAllSlots(@PathVariable int tournamentId) {
        log.debug("Fetching all slots for tournament: {}", tournamentId);

        List<SlotsDTO> slots = slotsService.getSlots(tournamentId);
        return ResponseEntity.ok(slots);
    }

    /**
     * Get user's booked slots
     * GET /api/slots/my-bookings
     */
    @GetMapping("/my-bookings")
    public ResponseEntity<List<SlotsDTO>> getMyBookings(Authentication authentication) {
        String firebaseUID = getAuthenticatedUserUID(authentication);

        log.debug("Fetching booked slots for user: {}", firebaseUID);

        List<SlotsDTO> mySlots = slotsService.getUserBookedSlots(firebaseUID);
        return ResponseEntity.ok(mySlots);
    }

    /**
     * Cancel slot booking (before tournament starts)
     * DELETE /api/slots/{slotId}/cancel
     */
    @DeleteMapping("/{slotId}/cancel")
    public ResponseEntity<Map<String, String>> cancelSlotBooking(
            @PathVariable int slotId,
            Authentication authentication) {

        String firebaseUID = getAuthenticatedUserUID(authentication);

        log.info("User {} cancelling slot booking: {}", firebaseUID, slotId);

        slotsService.cancelSlotBooking(slotId, firebaseUID);

        return ResponseEntity.ok(Map.of("message", "Slot booking cancelled successfully"));
    }

    private String getAuthenticatedUserUID(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return (String) authentication.getPrincipal();
    }
    // ADD TO SlotsController.java
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @DeleteMapping("/{slotId}/admin-cancel")
    public ResponseEntity<Map<String, String>> adminCancelSlot(
            @PathVariable int slotId,
            Authentication authentication) {

        String adminUID = (String) authentication.getPrincipal();
        slotsService.adminCancelSlotBooking(slotId, adminUID);

        return ResponseEntity.ok(Map.of("message", "Slot booking cancelled by admin"));
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PostMapping("/tournaments/{tournamentId}/generate")
    public ResponseEntity<Map<String, String>> generateSlots(
            @PathVariable int tournamentId,
            @RequestBody Map<String, Integer> request) {

        Integer maxPlayers = request.get("maxPlayers");
        slotsService.preGenerateSlots(tournamentId, maxPlayers);

        return ResponseEntity.ok(Map.of("message", "Slots generated successfully"));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSlotStats() {
        // Implement in SlotService
        // Return global slot statistics
        return ResponseEntity.ok(Map.of("message", "Implement slot statistics"));
    }
}