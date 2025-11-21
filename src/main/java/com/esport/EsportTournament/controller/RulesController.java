package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.service.RulesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/rules")
@RequiredArgsConstructor
public class RulesController {

    private final RulesService rulesService;

    /**
     * Get global rules
     * GET /api/admin/rules/global
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @GetMapping("/global")
    public ResponseEntity<List<String>> getGlobalRules() {
        log.debug("Fetching global rules");
        List<String> rules = rulesService.getGlobalRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * Update global rules
     * PUT /api/admin/rules/global
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/global")
    public ResponseEntity<List<String>> updateGlobalRules(
            @RequestBody Map<String, List<String>> request,
            Authentication authentication) {
        String adminUID = getAuthenticatedUserUID(authentication);
        List<String> rules = request.get("rules");
        if (rules == null) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Admin {} updating global rules", adminUID);
        List<String> updated = rulesService.updateGlobalRules(rules, adminUID);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get tournament-specific rules
     * GET /api/admin/rules/tournament/{tournamentId}
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<String>> getTournamentRules(@PathVariable int tournamentId) {
        log.debug("Fetching rules for tournament: {}", tournamentId);
        List<String> rules = rulesService.getTournamentRules(tournamentId);
        return ResponseEntity.ok(rules);
    }

    /**
     * Update tournament-specific rules
     * PUT /api/admin/rules/tournament/{tournamentId}
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<String>> updateTournamentRules(
            @PathVariable int tournamentId,
            @RequestBody Map<String, List<String>> request,
            Authentication authentication) {
        String adminUID = getAuthenticatedUserUID(authentication);
        List<String> rules = request.get("rules");
        if (rules == null) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Admin {} updating rules for tournament {}", adminUID, tournamentId);
        List<String> updated = rulesService.updateTournamentRules(tournamentId, rules, adminUID);
        return ResponseEntity.ok(updated);
    }

    /**
     * Apply global rules to tournament
     * POST /api/admin/rules/tournament/{tournamentId}/apply-global
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PostMapping("/tournament/{tournamentId}/apply-global")
    public ResponseEntity<List<String>> applyGlobalRulesToTournament(
            @PathVariable int tournamentId,
            Authentication authentication) {
        String adminUID = getAuthenticatedUserUID(authentication);
        log.info("Admin {} applying global rules to tournament {}", adminUID, tournamentId);
        List<String> rules = rulesService.applyGlobalRulesToTournament(tournamentId, adminUID);
        return ResponseEntity.ok(rules);
    }

    private String getAuthenticatedUserUID(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return (String) authentication.getPrincipal();
    }
}

