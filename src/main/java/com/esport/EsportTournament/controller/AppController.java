package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.service.AppConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppController {

    private final AppConfigService appConfigService;

    /**
     * Get app version information
     * GET /api/app/version
     */
    @GetMapping("/app/version")
    public ResponseEntity<Map<String, String>> getAppVersion() {
        log.debug("Fetching app version information");
        Map<String, String> version = appConfigService.getAppVersion();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(2)).cachePublic())
                .body(version);
    }

    /**
     * Update app version (admin only)
     * PUT /api/app/version
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/app/version")
    public ResponseEntity<Map<String, String>> updateAppVersion(
            @RequestBody Map<String, String> version,
            Authentication authentication) {
        String adminUID = getAuthenticatedUserUID(authentication);
        log.info("Admin {} updating app version", adminUID);
        Map<String, String> updated = appConfigService.updateAppVersion(version, adminUID);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get filter metadata for tournaments
     * GET /api/filters
     */
    @GetMapping("/filters")
    public ResponseEntity<Map<String, List<String>>> getFilters() {
        log.debug("Fetching filter metadata");
        Map<String, List<String>> filters = appConfigService.getFilters();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(filters);
    }

    /**
     * Update filters (admin only)
     * PUT /api/filters
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/filters")
    public ResponseEntity<Map<String, List<String>>> updateFilters(
            @RequestBody Map<String, List<String>> filters,
            Authentication authentication) {
        String adminUID = getAuthenticatedUserUID(authentication);
        log.info("Admin {} updating filters", adminUID);
        Map<String, List<String>> updated = appConfigService.updateFilters(filters, adminUID);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get Help & Support content (public).
     * GET /api/support
     */
    @GetMapping("/support")
    public ResponseEntity<Map<String, Object>> getHelpSupportContent() {
        log.debug("Fetching help/support content");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(appConfigService.getHelpSupportContent());
    }

    /**
     * Update Help & Support content (admin only).
     * PUT /api/support
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/support")
    public ResponseEntity<Map<String, Object>> updateHelpSupportContent(
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {
        String adminUID = getAuthenticatedUserUID(authentication);
        log.info("Admin {} updating help/support content", adminUID);
        return ResponseEntity.ok(appConfigService.updateHelpSupportContent(payload, adminUID));
    }

    /**
     * Get Terms & Conditions content (public).
     * GET /api/terms
     */
    @GetMapping("/terms")
    public ResponseEntity<Map<String, Object>> getTermsContent() {
        log.debug("Fetching terms content");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(appConfigService.getTermsContent());
    }

    /**
     * Update Terms & Conditions content (admin only).
     * PUT /api/terms
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/terms")
    public ResponseEntity<Map<String, Object>> updateTermsContent(
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {
        String adminUID = getAuthenticatedUserUID(authentication);
        log.info("Admin {} updating terms content", adminUID);
        return ResponseEntity.ok(appConfigService.updateTermsContent(payload, adminUID));
    }

    private String getAuthenticatedUserUID(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return (String) authentication.getPrincipal();
    }
}
