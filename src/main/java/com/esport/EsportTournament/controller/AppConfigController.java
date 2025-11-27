package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.service.AppConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class AppConfigController {

    private final AppConfigService appConfigService;

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> getAppVersion() {
        return ResponseEntity.ok(appConfigService.getAppVersion());
    }

    @PostMapping("/version")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateAppVersion(@RequestBody Map<String, String> version,
            @RequestAttribute(required = false) String adminUID) {
        return ResponseEntity.ok(appConfigService.updateAppVersion(version, adminUID != null ? adminUID : "admin"));
    }

    @GetMapping("/filters")
    public ResponseEntity<Map<String, List<String>>> getFilters() {
        return ResponseEntity.ok(appConfigService.getFilters());
    }

    @PostMapping("/filters")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, List<String>>> updateFilters(@RequestBody Map<String, List<String>> filters,
            @RequestAttribute(required = false) String adminUID) {
        return ResponseEntity.ok(appConfigService.updateFilters(filters, adminUID != null ? adminUID : "admin"));
    }

    @GetMapping("/logo")
    public ResponseEntity<Map<String, String>> getLogoUrl() {
        return ResponseEntity.ok(Map.of("logoUrl", appConfigService.getLogoUrl()));
    }

    @PostMapping("/logo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateLogoUrl(@RequestBody Map<String, String> body,
            @RequestAttribute(required = false) String adminUID) {
        String logoUrl = body.get("logoUrl");
        if (logoUrl == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity
                .ok(Map.of("logoUrl", appConfigService.updateLogoUrl(logoUrl, adminUID != null ? adminUID : "admin")));
    }
}
