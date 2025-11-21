package com.esport.EsportTournament.service;

import com.esport.EsportTournament.model.GlobalRules;
import com.esport.EsportTournament.model.Tournaments;
import com.esport.EsportTournament.repository.GlobalRulesRepo;
import com.esport.EsportTournament.repository.TournamentRepo;
import com.esport.EsportTournament.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RulesService {

    private final GlobalRulesRepo globalRulesRepo;
    private final TournamentRepo tournamentRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get global rules
     */
    @Transactional(readOnly = true)
    public List<String> getGlobalRules() {
        List<GlobalRules> rules = globalRulesRepo.findByIsActiveTrueOrderByDisplayOrderAsc();
        return rules.stream()
                .map(GlobalRules::getRuleText)
                .collect(Collectors.toList());
    }

    /**
     * Update global rules
     */
    @Transactional
    public List<String> updateGlobalRules(List<String> rules, String adminUID) {
        // Deactivate all existing rules
        List<GlobalRules> existingRules = globalRulesRepo.findAllByOrderByDisplayOrderAsc();
        for (GlobalRules rule : existingRules) {
            rule.setActive(false);
        }
        globalRulesRepo.saveAll(existingRules);

        // Create new rules
        List<GlobalRules> newRules = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            GlobalRules rule = new GlobalRules();
            rule.setRuleText(rules.get(i));
            rule.setDisplayOrder(i);
            rule.setActive(true);
            newRules.add(rule);
        }

        globalRulesRepo.saveAll(newRules);
        log.info("Admin {} updated global rules: {} rules", adminUID, newRules.size());

        return getGlobalRules();
    }

    /**
     * Get tournament-specific rules
     */
    @Transactional(readOnly = true)
    public List<String> getTournamentRules(int tournamentId) {
        Tournaments tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + tournamentId));

        if (tournament.getRules() == null || tournament.getRules().trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(tournament.getRules(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Error parsing tournament rules: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Update tournament-specific rules
     */
    @Transactional
    public List<String> updateTournamentRules(int tournamentId, List<String> rules, String adminUID) {
        Tournaments tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + tournamentId));

        try {
            String rulesJson = objectMapper.writeValueAsString(rules);
            tournament.setRules(rulesJson);
            tournamentRepo.save(tournament);
            log.info("Admin {} updated rules for tournament {}: {} rules", adminUID, tournamentId, rules.size());
        } catch (Exception e) {
            log.error("Error saving tournament rules: {}", e.getMessage());
            throw new RuntimeException("Failed to save tournament rules", e);
        }

        return getTournamentRules(tournamentId);
    }

    /**
     * Apply global rules to tournament
     */
    @Transactional
    public List<String> applyGlobalRulesToTournament(int tournamentId, String adminUID) {
        List<String> globalRules = getGlobalRules();
        return updateTournamentRules(tournamentId, globalRules, adminUID);
    }
}

