package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.dto.TournamentFilterDTO;
import com.esport.EsportTournament.dto.TournamentsDTO;
import com.esport.EsportTournament.dto.UpdateGameCredentials;
import com.esport.EsportTournament.model.Tournaments;
import com.esport.EsportTournament.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PostMapping
    public ResponseEntity<TournamentsDTO> createTournament(@RequestBody TournamentsDTO dto) {
        // Automatically set status to PLANNED when creating new tournament
        dto.setStatus(Tournaments.TournamentStatus.UPCOMING);
        return ResponseEntity.status(201).body(tournamentService.createTournament(dto));
    }

    @GetMapping
    public List<TournamentsDTO> getAllTournaments(){
        return tournamentService.getAllTournaments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentsDTO> getTournamentById(@PathVariable int id) {
        TournamentsDTO tournament = tournamentService.getTournamentById(id);
        return ResponseEntity.ok(tournament);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TournamentsDTO>> getTournamentsByStatus(@PathVariable String status) {
        Tournaments.TournamentStatus tournamentStatus = Tournaments.TournamentStatus.valueOf(status.toUpperCase());
        List<TournamentsDTO> tournaments = tournamentService.getTournamentsByStatus(tournamentStatus);
        return ResponseEntity.ok(tournaments);
    }

    @GetMapping("/{tournamentId}/credentials")
//    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> getTournamentCredentials(@PathVariable int tournamentId) {
        // The business logic is kept in the service layer.
        // The controller's job is to handle the HTTP request and response.
        Map<String, String> credentials = tournamentService.getGameCredentials(tournamentId);

        // ResponseEntity.ok() wraps the map in a response with an HTTP 200 OK status.
        return ResponseEntity.ok(credentials);
    }
//    @GetMapping("/filter")
//    public ResponseEntity<List<TournamentsDTO>> getTournamentsByStatus(@RequestBody TournamentFilterDTO filter) {
//        List<TournamentsDTO> tournaments = tournamentService.getTournamentByGameType(filter);
//        return ResponseEntity.ok(tournaments);
//    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/{id}/game-credentials")
    public ResponseEntity<TournamentsDTO> updateGameCredentials(
            @PathVariable int id,
            @RequestBody UpdateGameCredentials updateGameCredentials
    ) {
        return ResponseEntity.ok(tournamentService.updateGameCredentials(id, updateGameCredentials.getGameId(), updateGameCredentials.getGamePassword()));
    }

    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/{id}/start-time")
    public ResponseEntity<TournamentsDTO> updateStartTime(
            @PathVariable int id,
            @RequestParam String startTime // ISO format string
    ) {
        return ResponseEntity.ok(tournamentService.updateStartTime(id, LocalDateTime.parse(startTime)));
    }



    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/{id}/status")
    public ResponseEntity<TournamentsDTO> updateTournamentStatus(
            @PathVariable int id,
            @RequestBody Map<String, String> request) {

        String statusStr = request.get("status");
        // Support all tournament statuses including CANCELLED
        Tournaments.TournamentStatus status;
        try {
            status = Tournaments.TournamentStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        TournamentsDTO tournament = tournamentService.updateTournamentStatus(id, status);
        return ResponseEntity.ok(tournament);
    }



    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable int id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update tournament scoreboard
     * PUT /api/tournaments/{id}/scoreboard
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/{id}/scoreboard")
    public ResponseEntity<TournamentsDTO> updateTournamentScoreboard(
            @PathVariable int id,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scoreboardData = (List<Map<String, Object>>) request.get("scoreboard");
        TournamentsDTO updated = tournamentService.updateTournamentScoreboard(id, scoreboardData);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasAuthority('PERM_VIEW_ANALYTICS')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTournamentStats() {
        Map<String, Object> stats = tournamentService.getTournamentStats();
        return ResponseEntity.ok(stats);
    }
}