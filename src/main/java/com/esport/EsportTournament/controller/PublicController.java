package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.dto.TournamentsDTO;
import com.esport.EsportTournament.service.PlatformConfigService;
import com.esport.EsportTournament.service.TournamentService;
import com.esport.EsportTournament.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final TournamentService tournamentService;
    private final UserService userService;
    private final PlatformConfigService platformConfigService;

    /**
     * Get public tournament information (upcoming tournaments)
     */
    @GetMapping("/tournaments")
    public ResponseEntity<List<TournamentsDTO>> getPublicTournaments() {
        log.debug("Fetching public tournament information");

        try {
            List<TournamentsDTO> upcomingTournaments = tournamentService.getUpcomingTournaments();
            return ResponseEntity.ok(upcomingTournaments);
        } catch (Exception e) {
            log.error("Error fetching public tournaments", e);
            return ResponseEntity.ok(List.of()); // Return empty list instead of error
        }
    }

    /**
     * Get public tournament statistics
     */
    @GetMapping("/tournaments/stats")
    public ResponseEntity<Map<String, Object>> getPublicTournamentStats() {
        log.debug("Fetching public tournament statistics");

        try {
            Map<String, Object> stats = tournamentService.getTournamentStats();

            // Filter out sensitive information for public access
            Map<String, Object> publicStats = Map.of(
                    "totalTournaments", stats.get("total"),
                    "upcomingTournaments", stats.get("upcoming"),
                    "completedTournaments", stats.get("completed"),
                    "lastUpdated", LocalDateTime.now()
            );

            return ResponseEntity.ok(publicStats);
        } catch (Exception e) {
            log.error("Error fetching public tournament stats", e);
            return ResponseEntity.ok(Map.of(
                    "totalTournaments", 0,
                    "upcomingTournaments", 0,
                    "completedTournaments", 0,
                    "lastUpdated", LocalDateTime.now()
            ));
        }
    }

    /**
     * Get specific tournament details (public info only)
     */
    @GetMapping("/tournaments/{tournamentId}")
    public ResponseEntity<Map<String, Object>> getPublicTournamentDetails(@PathVariable int tournamentId) {
        log.debug("Fetching public details for tournament: {}", tournamentId);

        try {
            TournamentsDTO tournament = tournamentService.getTournamentById(tournamentId);

            // Return only public information
            // 🔥 CRITICAL: Ensure teamSize is never null
            String teamSize = tournament.getTeamSize();
            if (teamSize == null || teamSize.trim().isEmpty()) {
                teamSize = "SOLO";
            }
            
            // Use HashMap instead of Map.of() since we have more than 10 key-value pairs
            Map<String, Object> publicInfo = new HashMap<>();
            publicInfo.put("id", tournament.getId());
            publicInfo.put("name", tournament.getName());
            publicInfo.put("title", tournament.getTitle() != null ? tournament.getTitle() : tournament.getName());
            publicInfo.put("game", tournament.getGame());
            publicInfo.put("prizePool", tournament.getPrizePool());
            publicInfo.put("entryFee", tournament.getEntryFee());
            publicInfo.put("maxPlayers", tournament.getMaxPlayers());
            publicInfo.put("startTime", tournament.getStartTime());
            publicInfo.put("status", tournament.getStatus());
            publicInfo.put("teamSize", teamSize);
            publicInfo.put("imageLink", tournament.getImageLink() != null ? tournament.getImageLink() : "");

            return ResponseEntity.ok(publicInfo);
        } catch (Exception e) {
            log.error("Error fetching public tournament details for ID: {}", tournamentId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get platform statistics for public display
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPublicPlatformStats() {
        log.debug("Fetching public platform statistics");

        try {
            long totalUsers = userService.getActiveUsersCount();
            Map<String, Object> tournamentStats = tournamentService.getTournamentStats();

            Map<String, Object> publicStats = Map.of(
                    "activeUsers", totalUsers,
                    "totalTournaments", tournamentStats.get("total"),
                    "upcomingTournaments", tournamentStats.get("upcoming"),
                    "platformStatus", "OPERATIONAL",
                    "lastUpdated", LocalDateTime.now()
            );

            return ResponseEntity.ok(publicStats);
        } catch (Exception e) {
            log.error("Error fetching public platform stats", e);
            return ResponseEntity.ok(Map.of(
                    "activeUsers", 0,
                    "totalTournaments", 0,
                    "upcomingTournaments", 0,
                    "platformStatus", "MAINTENANCE",
                    "lastUpdated", LocalDateTime.now()
            ));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "service", "ESport Tournament Platform",
                "version", "1.0.0"
        );

        return ResponseEntity.ok(health);
    }

    /**
     * Get platform information and features
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getPlatformInfo() {
        try {
            Map<String, Object> info = platformConfigService.getPlatformInfo();
            // Add static features and supported games
            info.put("features", List.of(
                    "Tournament Registration",
                    "Digital Wallet System",
                    "Real-time Notifications",
                    "Secure Payments",
                    "Live Tournament Tracking"
            ));
            info.put("supportedGames", List.of(
                    "BGMI",
                    "Free Fire",
                    "Call of Duty Mobile",
                    "PUBG Mobile",
                    "Clash Royale"
            ));
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Error fetching platform info", e);
            // Return default info
            Map<String, Object> defaultInfo = new HashMap<>();
            defaultInfo.put("name", "ESport Tournament Platform");
            defaultInfo.put("description", "Competitive gaming tournament platform with wallet system");
            defaultInfo.put("supportEmail", "support@esporttournament.com");
            defaultInfo.put("termsUrl", "https://esporttournament.com/terms");
            defaultInfo.put("privacyUrl", "https://esporttournament.com/privacy");
            return ResponseEntity.ok(defaultInfo);
        }
    }

    /**
     * Update platform information (admin only)
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/info")
    public ResponseEntity<Map<String, Object>> updatePlatformInfo(
            @RequestBody Map<String, Object> info,
            Authentication authentication) {
        String adminUID = getAuthenticatedUserUID(authentication);
        log.info("Admin {} updating platform info", adminUID);
        Map<String, Object> updated = platformConfigService.updatePlatformInfo(info, adminUID);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get registration requirements for new users
     */
    @GetMapping("/registration/requirements")
    public ResponseEntity<Map<String, Object>> getRegistrationRequirements() {
        try {
            Map<String, Object> requirements = platformConfigService.getRegistrationRequirements();
            // Add static optional fields and benefits
            requirements.put("optional", List.of(
                    "Profile picture",
                    "Display name preference"
            ));
            requirements.put("benefits", List.of(
                    "Access to tournaments",
                    "Digital wallet for transactions",
                    "Real-time notifications",
                    "Tournament history tracking"
            ));
            return ResponseEntity.ok(requirements);
        } catch (Exception e) {
            log.error("Error fetching registration requirements", e);
            // Return default requirements
            Map<String, Object> defaultReqs = new HashMap<>();
            defaultReqs.put("minimumAge", 13);
            defaultReqs.put("requiredDocuments", List.of("Valid email address", "Firebase authentication", "Unique username"));
            defaultReqs.put("termsAndConditions", "Must be 13+ years old. One account per person.");
            defaultReqs.put("privacyPolicy", "Your data is protected and will not be shared with third parties.");
            return ResponseEntity.ok(defaultReqs);
        }
    }

    /**
     * Update registration requirements (admin only)
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/registration/requirements")
    public ResponseEntity<Map<String, Object>> updateRegistrationRequirements(
            @RequestBody Map<String, Object> requirements,
            Authentication authentication) {
        String adminUID = getAuthenticatedUserUID(authentication);
        log.info("Admin {} updating registration requirements", adminUID);
        Map<String, Object> updated = platformConfigService.updateRegistrationRequirements(requirements, adminUID);
        return ResponseEntity.ok(updated);
    }

    private String getAuthenticatedUserUID(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return (String) authentication.getPrincipal();
    }

    /**
     * Get tournament categories and types
     */
    @GetMapping("/tournaments/categories")
    public ResponseEntity<Map<String, Object>> getTournamentCategories() {
        Map<String, Object> categories = Map.of(
                "gameTypes", List.of(
                        Map.of("name", "BGMI", "description", "Battlegrounds Mobile India"),
                        Map.of("name", "Free Fire", "description", "Free Fire Battle Royale"),
                        Map.of("name", "COD Mobile", "description", "Call of Duty Mobile"),
                        Map.of("name", "PUBG Mobile", "description", "PUBG Mobile")
                ),
                "tournamentTypes", List.of(
                        Map.of("type", "Solo", "description", "Individual player tournaments"),
                        Map.of("type", "Duo", "description", "2-player team tournaments"),
                        Map.of("type", "Squad", "description", "4-player team tournaments")
                ),
                "prizeRanges", List.of(
                        "₹100 - ₹500",
                        "₹500 - ₹2000",
                        "₹2000 - ₹10000",
                        "₹10000+"
                )
        );

        return ResponseEntity.ok(categories);
    }
}