package com.esport.EsportTournament.service;

import com.esport.EsportTournament.model.AppConfig;
import com.esport.EsportTournament.repository.AppConfigRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppConfigService {

    private final AppConfigRepo appConfigRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get app version
     */
    @Transactional(readOnly = true)
    @Cacheable("app_version")
    public Map<String, String> getAppVersion() {
        Map<String, String> version = new HashMap<>();
        version.put("minSupported", getConfigValue("app_version_min_supported", "1.1.0"));
        version.put("latest", getConfigValue("app_version_latest", "1.3.2"));
        version.put("playStoreUrl", getConfigValue("app_version_play_store_url",
                "https://play.google.com/store/apps/details?id=com.esport.tournament"));
        return version;
    }

    /**
     * Update app version
     */
    @Transactional
    @CacheEvict(value = "app_version", allEntries = true)
    public Map<String, String> updateAppVersion(Map<String, String> version, String adminUID) {
        if (version.containsKey("minSupported")) {
            setConfigValue("app_version_min_supported", version.get("minSupported"), adminUID);
        }
        if (version.containsKey("latest")) {
            setConfigValue("app_version_latest", version.get("latest"), adminUID);
        }
        if (version.containsKey("playStoreUrl")) {
            setConfigValue("app_version_play_store_url", version.get("playStoreUrl"), adminUID);
        }
        log.info("Admin {} updated app version", adminUID);
        return getAppVersion();
    }

    /**
     * Get filters
     */
    @Transactional(readOnly = true)
    @Cacheable("app_filters")
    public Map<String, List<String>> getFilters() {
        String filtersJson = getConfigValue("filters", null);
        if (filtersJson == null) {
            // Return default filters
            return getDefaultFilters();
        }
        try {
            return objectMapper.readValue(filtersJson, new TypeReference<Map<String, List<String>>>() {
            });
        } catch (Exception e) {
            log.warn("Error parsing filters: {}", e.getMessage());
            return getDefaultFilters();
        }
    }

    /**
     * Update filters
     */
    @Transactional
    @CacheEvict(value = "app_filters", allEntries = true)
    public Map<String, List<String>> updateFilters(Map<String, List<String>> filters, String adminUID) {
        try {
            String filtersJson = objectMapper.writeValueAsString(filters);
            setConfigValue("filters", filtersJson, adminUID);
            log.info("Admin {} updated filters", adminUID);
        } catch (Exception e) {
            log.error("Error saving filters: {}", e.getMessage());
            throw new RuntimeException("Failed to save filters", e);
        }
        return getFilters();
    }

    @Transactional(readOnly = true)
    @Cacheable("app_logo")
    public String getLogoUrl() {
        return getConfigValue("app_logo_url", "");
    }

    @Transactional
    @CacheEvict(value = "app_logo", allEntries = true)
    public String updateLogoUrl(String logoUrl, String adminUID) {
        setConfigValue("app_logo_url", logoUrl, adminUID);
        return logoUrl;
    }

    private String getConfigValue(String key, String defaultValue) {
        return appConfigRepo.findByConfigKey(key)
                .map(AppConfig::getConfigValue)
                .orElse(defaultValue);
    }

    private void setConfigValue(String key, String value, String adminUID) {
        AppConfig config = appConfigRepo.findByConfigKey(key)
                .orElse(new AppConfig());
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType("STRING");
        config.setUpdatedBy(adminUID);
        appConfigRepo.save(config);
    }

    private Map<String, List<String>> getDefaultFilters() {
        Map<String, List<String>> filters = new HashMap<>();
        filters.put("games", List.of("Free Fire", "PUBG", "COD Mobile", "BGMI", "Clash Royale"));
        filters.put("teamSizes", List.of("Solo", "Duo", "Squad", "Hexa"));
        filters.put("maps", List.of("Bermuda", "Purgatory", "Kalahari", "Alpine", "NeXTerra"));
        filters.put("timeSlots", List.of("6:00-6:30 PM", "7:00-8:00 PM", "8:00-9:00 PM", "9:00-10:00 PM"));
        return filters;
    }
}
