package com.esport.EsportTournament.service;

import com.esport.EsportTournament.dto.SlotsDTO;
import com.esport.EsportTournament.dto.TournamentsDTO;
import com.esport.EsportTournament.exception.ResourceNotFoundException;
import com.esport.EsportTournament.model.Tournaments;
import com.esport.EsportTournament.model.TournamentResult;
import com.esport.EsportTournament.repository.TournamentRepo;
import com.esport.EsportTournament.repository.TournamentResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepo tournamentRepo;
    private final TournamentResultRepository tournamentResultRepository;
    private final SlotService slotService;
    private final NotificationService notificationService;
    private final com.esport.EsportTournament.util.EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create a new tournament
     * FIXED: Enhanced validation and error handling
     */
    @Transactional
    @CacheEvict(value = { "tournaments", "upcoming_tournaments", "tournament_stats" }, allEntries = true)
    public TournamentsDTO createTournament(TournamentsDTO dto) {
        log.info("Creating tournament with teamSize: {}", dto.getTeamSize());

        Tournaments tournament = new Tournaments();
        tournament.setName(dto.getName());
        tournament.setPrizePool(dto.getPrizePool());
        tournament.setEntryFees(dto.getEntryFee());
        tournament.setImageLink(dto.getImageLink());
        tournament.setMapType(dto.getMap());
        tournament.setStartTime(dto.getStartTime());

        // 🔥 CRITICAL: Set team size as string
        String teamSize = dto.getTeamSize();
        if (teamSize == null || teamSize.trim().isEmpty()) {
            teamSize = "SOLO";
        }
        tournament.setTeamSize(teamSize.toUpperCase()); // Normalize to uppercase

        tournament.setStatus(dto.getStatus());
        tournament.setMaxPlayers(dto.getMaxPlayers());
        tournament.setGame(dto.getGame());
        // Encrypt credentials
        tournament.setGameId(encryptionUtil.encrypt(dto.getGameId()));
        tournament.setGamePassword(encryptionUtil.encrypt(dto.getGamePassword()));

        // Save rules as JSON string
        if (dto.getRules() != null && !dto.getRules().isEmpty()) {
            try {
                tournament.setRules(objectMapper.writeValueAsString(dto.getRules()));
            } catch (Exception e) {
                log.warn("Error serializing rules: {}", e.getMessage());
            }
        }

        // Set prize fields
        tournament.setPerKillReward(dto.getPerKillReward());
        tournament.setFirstPrize(dto.getFirstPrize());
        tournament.setSecondPrize(dto.getSecondPrize());
        tournament.setThirdPrize(dto.getThirdPrize());
        tournament.setStreamUrl(dto.getStreamUrl());

        Tournaments saved = tournamentRepo.save(tournament);

        log.info("✅ Tournament created: ID={}, TeamSize={}, Players/Team={}",
                saved.getId(), saved.getTeamSize(), saved.getPlayersPerTeam());

        // Pre-generate slots
        slotService.preGenerateSlots(saved.getId(), saved.getMaxPlayers());

        return mapToDTO(saved);
    }

    /**
     * Update Game ID and Password for a tournament
     */

    public Map<String, String> getGameCredentials(int tournamentId) {
        log.info("🎮 Fetching game credentials for tournament ID: {}", tournamentId);

        TournamentsDTO tournamentsDTO = getTournamentById(tournamentId);

        // 🔥 CRITICAL FIX: Check for null values before creating map
        // Decrypt credentials
        String gameId = encryptionUtil.decrypt(tournamentsDTO.getGameId());
        String gamePassword = encryptionUtil.decrypt(tournamentsDTO.getGamePassword());

        // Log the values for debugging (masked)
        log.debug("Tournament {} - GameId: {}, GamePassword: {}",
                tournamentId,
                gameId != null ? "***SET***" : "NULL",
                gamePassword != null ? "***SET***" : "NULL");

        // Option 1: Return empty strings instead of null
        return Map.of(
                "gameId", gameId != null ? gameId : "",
                "gamePassword", gamePassword != null ? gamePassword : "");

        /*
         * Option 2: Use HashMap if you want to allow nulls
         * Map<String, String> credentials = new HashMap<>();
         * credentials.put("gameId", gameId);
         * credentials.put("gamePassword", gamePassword);
         * return credentials;
         */

        /*
         * Option 3: Throw exception if credentials not set
         * if (gameId == null || gameId.trim().isEmpty() ||
         * gamePassword == null || gamePassword.trim().isEmpty()) {
         * throw new IllegalStateException(
         * "Game credentials not set for tournament ID: " + tournamentId +
         * ". Please ask admin to set credentials first."
         * );
         * }
         * 
         * return Map.of(
         * "gameId", gameId,
         * "gamePassword", gamePassword
         * );
         */
    }

    @Transactional
    @CacheEvict(value = { "tournaments", "upcoming_tournaments", "tournament", "tournament_stats" }, allEntries = true)
    public TournamentsDTO updateGameCredentials(int tournamentId, String gameId, String gamePassword) {
        log.info("Updating game credentials for tournament ID: {}", tournamentId);

        if (gameId == null || gameId.trim().isEmpty()) {
            throw new IllegalArgumentException("Game ID cannot be null or empty");
        }

        if (gamePassword == null || gamePassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Game password cannot be null or empty");
        }

        Tournaments tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with ID: " + tournamentId));

        tournament.setGameId(encryptionUtil.encrypt(gameId));
        tournament.setGamePassword(encryptionUtil.encrypt(gamePassword));
        tournament.setUpdatedAt(LocalDateTime.now());

        Tournaments updated = tournamentRepo.save(tournament);
        log.info("Game credentials updated successfully for tournament ID: {}", tournamentId);

        List<SlotsDTO> slotList = slotService.getSlots(tournamentId);
        List<String> participants = slotList.stream().map(SlotsDTO::getFirebaseUserUID).toList();

        notificationService.sendGameCredentials(tournamentId, gameId, gamePassword, participants);

        return mapToDTO(updated);
    }

    /**
     * Change the start time of a tournament
     */
    @Transactional
    @CacheEvict(value = { "tournaments", "upcoming_tournaments", "tournament", "tournament_stats" }, allEntries = true)
    public TournamentsDTO updateStartTime(int tournamentId, LocalDateTime newStartTime) {
        log.info("Updating start time for tournament ID: {}", tournamentId);

        if (newStartTime == null) {
            throw new IllegalArgumentException("New start time cannot be null");
        }

        if (newStartTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("New start time cannot be in the past");
        }

        Tournaments tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with ID: " + tournamentId));

        // ADDED: Check if tournament can still be modified
        if (tournament.getStatus() == Tournaments.TournamentStatus.ONGOING ||
                tournament.getStatus() == Tournaments.TournamentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot modify start time for ongoing or completed tournaments");
        }

        tournament.setStartTime(newStartTime);
        tournament.setUpdatedAt(LocalDateTime.now());

        Tournaments updated = tournamentRepo.save(tournament);
        log.info("Start time updated successfully for tournament ID: {}", tournamentId);

        return mapToDTO(updated);
    }

    /**
     * Get all tournaments
     */
    @Transactional(readOnly = true)
    @Cacheable("tournaments")
    public List<TournamentsDTO> getAllTournaments() {
        log.debug("Fetching all tournaments");
        return tournamentRepo.findAllByOrderByStartTimeDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ADDED: Get tournaments by status
     */
    @Transactional(readOnly = true)
    public List<TournamentsDTO> getTournamentsByStatus(Tournaments.TournamentStatus status) {
        log.debug("Fetching tournaments with status: {}", status);
        return tournamentRepo.findByStatusOrderByStartTimeAsc(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ADDED: Get upcoming tournaments (for user view)
     */
    @Transactional(readOnly = true)
    @Cacheable("upcoming_tournaments")
    public List<TournamentsDTO> getUpcomingTournaments() {
        return getTournamentsByStatus(Tournaments.TournamentStatus.UPCOMING);
    }

    /**
     * ADDED: Get tournament by ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "tournament", key = "#tournamentId")
    public TournamentsDTO getTournamentById(int tournamentId) {
        log.debug("Fetching tournament with ID: {}", tournamentId);

        Tournaments tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with ID: " + tournamentId));

        return mapToDTO(tournament);
    }

    // @Transactional(readOnly = true)
    // public List<TournamentsDTO> getTournamentByGameType(TournamentFilterDTO
    // tournamentFilterDTO) {
    // log.debug("Fetching tournament with GameType OR StartTime: {}",
    // tournamentFilterDTO.getGameType() + tournamentFilterDTO.getStartTime());
    //
    //
    // return
    // tournamentRepo.findByGameORStartTime(tournamentFilterDTO.getGameType(),tournamentFilterDTO.getStartTime()).stream()
    // .map(this::mapToDTO)
    // .collect(Collectors.toList());
    // }
    /**
     * ADDED: Update tournament status
     */
    @Transactional
    @CacheEvict(value = { "tournaments", "upcoming_tournaments", "tournament", "tournament_stats" }, allEntries = true)
    public TournamentsDTO updateTournamentStatus(int tournamentId, Tournaments.TournamentStatus newStatus) {
        log.info("Updating status for tournament ID: {} to {}", tournamentId, newStatus);

        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }

        Tournaments tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with ID: " + tournamentId));

        // ✅ NEW: Trigger refunds if cancelled
        if (newStatus == Tournaments.TournamentStatus.CANCELLED &&
                tournament.getStatus() != Tournaments.TournamentStatus.CANCELLED) {
            log.warn("🚨 Tournament {} is being CANCELLED. Initiating refund process...", tournamentId);
            slotService.processTournamentCancellation(tournamentId);
        }

        tournament.setStatus(newStatus);
        tournament.setUpdatedAt(LocalDateTime.now());

        Tournaments updated = tournamentRepo.save(tournament);
        log.info("Status updated successfully for tournament ID: {}", tournamentId);

        return mapToDTO(updated);
    }

    /**
     * Delete a tournament
     */
    @Transactional
    @CacheEvict(value = { "tournaments", "upcoming_tournaments", "tournament", "tournament_stats" }, allEntries = true)
    public void deleteTournament(int tournamentId) {
        log.warn("Deleting tournament ID: {}", tournamentId);

        Tournaments tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with ID: " + tournamentId));

        // ADDED: Check if tournament can be deleted
        if (tournament.getStatus() == Tournaments.TournamentStatus.ONGOING) {
            throw new IllegalStateException("Cannot delete an ongoing tournament");
        }

        tournamentRepo.deleteById(tournamentId);
        log.info("Tournament deleted successfully with ID: {}", tournamentId);
    }

    /**
     * Update tournament scoreboard
     */
    @Transactional
    @CacheEvict(value = { "tournaments", "upcoming_tournaments", "tournament", "tournament_stats" }, allEntries = true)
    public TournamentsDTO updateTournamentScoreboard(int tournamentId, List<Map<String, Object>> scoreboardData) {
        log.info("Updating scoreboard for tournament: {}", tournamentId);

        Tournaments tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + tournamentId));

        // Convert scoreboard data to DTO format
        List<TournamentsDTO.ScoreboardEntry> scoreboard = scoreboardData.stream()
                .map(data -> {
                    TournamentsDTO.ScoreboardEntry entry = new TournamentsDTO.ScoreboardEntry();
                    entry.setPlayerName((String) data.get("playerName"));
                    entry.setTeamName((String) data.get("teamName"));
                    entry.setKills(data.get("kills") != null ? ((Number) data.get("kills")).intValue() : 0);
                    entry.setCoinsEarned(
                            data.get("coinsEarned") != null ? ((Number) data.get("coinsEarned")).intValue() : 0);
                    entry.setPlacement(data.get("placement") != null ? ((Number) data.get("placement")).intValue() : 0);
                    return entry;
                })
                .collect(Collectors.toList());

        // 🔥 CRITICAL FIX: Persist scoreboard to database
        // First, clear existing results for this tournament to avoid duplicates if
        // updated multiple times
        tournamentResultRepository.deleteByTournament_Id(tournamentId);

        List<TournamentResult> results = new ArrayList<>();

        // We need to map player names to Firebase UIDs.
        // Ideally, the frontend should send UIDs. If not, we try to match via Slots.
        List<SlotsDTO> slots = slotService.getSlots(tournamentId);
        Map<String, String> playerNameToUidMap = slots.stream()
                .filter(s -> s.getPlayerName() != null && s.getFirebaseUserUID() != null)
                .collect(Collectors.toMap(SlotsDTO::getPlayerName, SlotsDTO::getFirebaseUserUID, (a, b) -> a));

        for (TournamentsDTO.ScoreboardEntry entry : scoreboard) {
            // 🔥 CRITICAL FIX: Prioritize UID from input if available
            String uid = null;

            // Try to find UID in the original map data first
            for (Map<String, Object> data : scoreboardData) {
                if (entry.getPlayerName().equals(data.get("playerName"))) {
                    if (data.containsKey("firebaseUserUID")) {
                        uid = (String) data.get("firebaseUserUID");
                    } else if (data.containsKey("uid")) {
                        uid = (String) data.get("uid");
                    }
                    break;
                }
            }

            // Fallback to name lookup if not found in input
            if (uid == null) {
                uid = playerNameToUidMap.get(entry.getPlayerName());
            }

            // If we can't find the UID by name, we might have an issue.
            // For now, we skip if UID is missing, or we could try to look it up if passed
            // in data.
            // Let's check if the input data has 'firebaseUserUID' or 'uid'
            if (uid == null) {
                // Try to find in the original map if passed
                // This requires the frontend to send it.
                // For now, we log a warning.
                log.warn("⚠️ Could not find UID for player: {} in tournament: {}", entry.getPlayerName(), tournamentId);
                continue;
            }

            results.add(TournamentResult.builder()
                    .tournament(tournament)
                    .firebaseUserUID(uid)
                    .playerName(entry.getPlayerName())
                    .teamName(entry.getTeamName())
                    .kills(entry.getKills())
                    .placement(entry.getPlacement())
                    .coinsEarned(entry.getCoinsEarned())
                    .build());
        }

        if (!results.isEmpty()) {
            tournamentResultRepository.saveAll(results);
            log.info("✅ Saved {} tournament results to database", results.size());
        }

        // Store scoreboard as JSON in a separate field or extend the model
        // For now, we'll store it in the DTO only (can be extended to database later)
        TournamentsDTO dto = mapToDTO(tournament);
        dto.setScoreboard(scoreboard);

        log.info("Scoreboard updated for tournament {}: {} entries", tournamentId, scoreboard.size());
        return dto;
    }

    /**
     * ADDED: Get tournament statistics
     */
    @Transactional(readOnly = true)
    @Cacheable("tournament_stats")
    public java.util.Map<String, Object> getTournamentStats() {
        long total = tournamentRepo.count();
        long upcoming = tournamentRepo.countByStatus(Tournaments.TournamentStatus.UPCOMING);
        long ongoing = tournamentRepo.countByStatus(Tournaments.TournamentStatus.ONGOING);
        long completed = tournamentRepo.countByStatus(Tournaments.TournamentStatus.COMPLETED);
        long cancelled = tournamentRepo.countByStatus(Tournaments.TournamentStatus.CANCELLED);

        return java.util.Map.of(
                "total", total,
                "upcoming", upcoming,
                "ongoing", ongoing,
                "completed", completed,
                "cancelled", cancelled);
    }

    /**
     * Map entity to DTO with participants and additional fields
     */
    private TournamentsDTO mapToDTO(Tournaments t) {
        TournamentsDTO dto = new TournamentsDTO();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setPrizePool(t.getPrizePool());
        dto.setEntryFee(t.getEntryFees());
        dto.setImageLink(t.getImageLink());
        dto.setMap(t.getMapType());
        dto.setGame(t.getGame());
        dto.setMaxPlayers(t.getMaxPlayers());
        dto.setStartTime(t.getStartTime());

        // 🔥 CRITICAL: Ensure teamSize is never null - default to SOLO if null
        String teamSize = t.getTeamSize();
        if (teamSize == null || teamSize.trim().isEmpty()) {
            teamSize = "SOLO";
            log.warn("Tournament {} has null/empty teamSize, defaulting to SOLO", t.getId());
        } else {
            // Normalize to uppercase for consistency
            teamSize = teamSize.trim().toUpperCase();
        }
        dto.setTeamSize(teamSize);

        dto.setStatus(t.getStatus());
        dto.setGameId(t.getGameId());
        dto.setGamePassword(t.getGamePassword());

        // Parse and set rules from JSON string
        if (t.getRules() != null && !t.getRules().trim().isEmpty()) {
            try {
                List<String> rulesList = objectMapper.readValue(t.getRules(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                        });
                dto.setRules(rulesList);
            } catch (Exception e) {
                log.warn("Error parsing rules for tournament {}: {}", t.getId(), e.getMessage());
                dto.setRules(new ArrayList<>());
            }
        } else {
            dto.setRules(new ArrayList<>());
        }

        // Set prize fields
        dto.setPerKillReward(t.getPerKillReward());
        dto.setFirstPrize(t.getFirstPrize());
        dto.setSecondPrize(t.getSecondPrize());
        dto.setThirdPrize(t.getThirdPrize());
        dto.setStreamUrl(t.getStreamUrl());

        // Populate participants from slots
        try {
            List<SlotsDTO> slots = slotService.getSlots(t.getId());
            List<TournamentsDTO.ParticipantInfo> participants = slots.stream()
                    .filter(slot -> slot.getStatus() == com.esport.EsportTournament.model.Slots.SlotStatus.BOOKED)
                    .map(slot -> new TournamentsDTO.ParticipantInfo(
                            slot.getPlayerName(),
                            slot.getSlotNumber(),
                            slot.getFirebaseUserUID()))
                    .collect(Collectors.toList());
            dto.setParticipants(participants);
            dto.setRegisteredPlayers(participants.size());
        } catch (Exception e) {
            log.warn("Error fetching participants for tournament {}: {}", t.getId(), e.getMessage());
            dto.setParticipants(new ArrayList<>());
            dto.setRegisteredPlayers(0);
        }

        // Scoreboard and prize fields are optional and can be set by admin
        // They will be populated from database if columns exist, or left empty
        try {
            List<TournamentResult> results = tournamentResultRepository.findByTournament_Id(t.getId());
            List<TournamentsDTO.ScoreboardEntry> scoreboard = results.stream()
                    .map(r -> new TournamentsDTO.ScoreboardEntry(
                            r.getPlayerName(),
                            r.getTeamName(),
                            r.getKills(),
                            r.getCoinsEarned(),
                            r.getPlacement()))
                    .collect(Collectors.toList());
            dto.setScoreboard(scoreboard);
        } catch (Exception e) {
            log.warn("Error fetching scoreboard for tournament {}: {}", t.getId(), e.getMessage());
            dto.setScoreboard(new ArrayList<>());
        }

        return dto;
    }
}