package com.esport.EsportTournament.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/banned")
@PreAuthorize("hasRole('BANNED')")
@RequiredArgsConstructor
public class BannedUserController {

    /**
     * Get ban information for banned user
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBanStatus(Authentication authentication) {
        String firebaseUID = getAuthenticatedUserUID(authentication);

        log.debug("Fetching ban status for user: {}", firebaseUID);

        Map<String, Object> banInfo = Map.of(
                "status", "BANNED",
                "message", "Your account has been temporarily suspended.",
                "appealAvailable", true,
                "supportEmail", "support@esporttournament.com",
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.ok(banInfo);
    }

    /**
     * Submit ban appeal
     */
    @PostMapping("/appeal")
    public ResponseEntity<Map<String, String>> submitAppeal(
            @RequestBody Map<String, String> appealRequest,
            Authentication authentication) {

        String firebaseUID = getAuthenticatedUserUID(authentication);
        String reason = appealRequest.get("reason");
        String message = appealRequest.get("message");

        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Appeal message cannot be empty");
        }

        log.info("Ban appeal submitted by user: {} - Reason: {} - Message: {}",
                firebaseUID, reason, message.substring(0, Math.min(message.length(), 100)));

        // In a real implementation, you'd save this to an appeals database table
        // For now, just log and return success

        return ResponseEntity.ok(Map.of(
                "message", "Your appeal has been submitted and will be reviewed within 24-48 hours.",
                "appealId", "APP_" + System.currentTimeMillis(),
                "status", "PENDING_REVIEW"
        ));
    }

    /**
     * Get appeal guidelines
     */
    @GetMapping("/appeal/guidelines")
    public ResponseEntity<Map<String, Object>> getAppealGuidelines() {
        Map<String, Object> guidelines = Map.of(
                "title", "Ban Appeal Guidelines",
                "guidelines", java.util.List.of(
                        "Be honest and respectful in your appeal",
                        "Provide specific details about the incident",
                        "Explain what you've learned from the situation",
                        "Commit to following platform rules in the future"
                ),
                "processTime", "24-48 hours",
                "appealLimit", "One appeal per ban incident",
                "contactEmail", "appeals@esporttournament.com"
        );

        return ResponseEntity.ok(guidelines);
    }

    private String getAuthenticatedUserUID(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return (String) authentication.getPrincipal();
    }
}