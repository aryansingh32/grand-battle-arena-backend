package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppConfigRepo extends JpaRepository<AppConfig, Integer> {
    Optional<AppConfig> findByConfigKey(String configKey);
}

