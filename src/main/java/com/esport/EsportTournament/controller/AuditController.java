package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.model.AuditLog;
import com.esport.EsportTournament.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogService auditLogService;

    /**
     * Get my audit logs (User)
     */
    @GetMapping("/my")
    public ResponseEntity<List<AuditLog>> getMyLogs(Authentication authentication,
            @RequestParam(defaultValue = "50") int limit) {
        String userId = authentication.getName();
        return ResponseEntity.ok(auditLogService.getLogsByUser(userId, limit));
    }

    /**
     * Get all logs (Admin)
     */
    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @GetMapping("/all")
    public ResponseEntity<List<AuditLog>> getAllLogs(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(auditLogService.getRecentLogs(limit));
    }

    /**
     * Get logs by user (Admin)
     */
    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getLogsByUser(@PathVariable String userId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(auditLogService.getLogsByUser(userId, limit));
    }

    /**
     * Get logs by category (Admin)
     */
    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @GetMapping("/category/{category}")
    public ResponseEntity<List<AuditLog>> getLogsByCategory(@PathVariable String category,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(auditLogService.getLogsByCategory(category, limit));
    }
}
