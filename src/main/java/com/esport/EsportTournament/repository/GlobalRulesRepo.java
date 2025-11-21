package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.GlobalRules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlobalRulesRepo extends JpaRepository<GlobalRules, Integer> {
    List<GlobalRules> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<GlobalRules> findAllByOrderByDisplayOrderAsc();
}

