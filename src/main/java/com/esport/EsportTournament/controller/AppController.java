package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.service.AppConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
        return ResponseEntity.ok(version);
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
        return ResponseEntity.ok(filters);
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

    private String getAuthenticatedUserUID(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return (String) authentication.getPrincipal();
    }
}

