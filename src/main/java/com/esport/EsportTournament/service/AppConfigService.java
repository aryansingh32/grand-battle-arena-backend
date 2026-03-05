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
import java.util.regex.Pattern;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppConfigService {

    private static final int MAX_JSON_LENGTH = 50_000;
    private static final int MAX_LIST_SIZE = 100;
    private static final int MAX_STRING_LENGTH = 500;
    private static final Pattern SIMPLE_VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

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
            String minSupported = safeTrim(version.get("minSupported"));
            if (!isValidVersion(minSupported)) {
                throw new IllegalArgumentException("Invalid minSupported version format. Expected x.y.z");
            }
            setConfigValue("app_version_min_supported", minSupported, adminUID);
        }
        if (version.containsKey("latest")) {
            String latest = safeTrim(version.get("latest"));
            if (!isValidVersion(latest)) {
                throw new IllegalArgumentException("Invalid latest version format. Expected x.y.z");
            }
            setConfigValue("app_version_latest", latest, adminUID);
        }
        if (version.containsKey("playStoreUrl")) {
            String playStoreUrl = safeTrim(version.get("playStoreUrl"));
            if (playStoreUrl.length() > MAX_STRING_LENGTH) {
                throw new IllegalArgumentException("playStoreUrl is too long");
            }
            setConfigValue("app_version_play_store_url", playStoreUrl, adminUID);
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
        validateFilterPayload(filters);
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

    /**
     * Get dynamic Help & Support content (contacts + FAQs).
     */
    @Transactional(readOnly = true)
    @Cacheable("help_support_content")
    public Map<String, Object> getHelpSupportContent() {
        String rawJson = getConfigValue("help_support_content", null);
        if (rawJson == null || rawJson.isBlank()) {
            return getDefaultHelpSupportContent();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("Error parsing help support content: {}", e.getMessage());
            return getDefaultHelpSupportContent();
        }
    }

    /**
     * Update dynamic Help & Support content (admin only).
     */
    @Transactional
    @CacheEvict(value = "help_support_content", allEntries = true)
    public Map<String, Object> updateHelpSupportContent(Map<String, Object> payload, String adminUID) {
        validateJsonPayload(payload, "help support content");
        try {
            String json = objectMapper.writeValueAsString(payload);
            setConfigValue("help_support_content", json, adminUID);
            log.info("Admin {} updated help/support content", adminUID);
            return getHelpSupportContent();
        } catch (Exception e) {
            log.error("Error saving help support content: {}", e.getMessage());
            throw new RuntimeException("Failed to save help/support content", e);
        }
    }

    /**
     * Get Terms & Conditions content.
     */
    @Transactional(readOnly = true)
    @Cacheable("terms_content")
    public Map<String, Object> getTermsContent() {
        String rawJson = getConfigValue("terms_content", null);
        if (rawJson == null || rawJson.isBlank()) {
            return getDefaultTermsContent();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("Error parsing terms content: {}", e.getMessage());
            return getDefaultTermsContent();
        }
    }

    /**
     * Update Terms & Conditions content (admin only).
     */
    @Transactional
    @CacheEvict(value = "terms_content", allEntries = true)
    public Map<String, Object> updateTermsContent(Map<String, Object> payload, String adminUID) {
        validateJsonPayload(payload, "terms content");
        try {
            String json = objectMapper.writeValueAsString(payload);
            setConfigValue("terms_content", json, adminUID);
            log.info("Admin {} updated terms content", adminUID);
            return getTermsContent();
        } catch (Exception e) {
            log.error("Error saving terms content: {}", e.getMessage());
            throw new RuntimeException("Failed to save terms content", e);
        }
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

    private boolean isValidVersion(String version) {
        return version != null && SIMPLE_VERSION_PATTERN.matcher(version).matches();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void validateFilterPayload(Map<String, List<String>> filters) {
        if (filters == null || filters.isEmpty()) {
            throw new IllegalArgumentException("Filters payload cannot be empty");
        }
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            List<String> values = entry.getValue();
            if (key.isBlank()) {
                throw new IllegalArgumentException("Filter key cannot be blank");
            }
            if (values == null || values.isEmpty()) {
                throw new IllegalArgumentException("Filter list cannot be empty for key: " + key);
            }
            if (values.size() > MAX_LIST_SIZE) {
                throw new IllegalArgumentException("Too many filter values for key: " + key);
            }
            for (String item : values) {
                if (item == null || item.trim().isEmpty()) {
                    throw new IllegalArgumentException("Filter value cannot be empty for key: " + key);
                }
                if (item.length() > MAX_STRING_LENGTH) {
                    throw new IllegalArgumentException("Filter value too long for key: " + key);
                }
            }
        }
    }

    private void validateJsonPayload(Map<String, Object> payload, String name) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException(name + " payload cannot be empty");
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            if (json.length() > MAX_JSON_LENGTH) {
                throw new IllegalArgumentException(name + " payload is too large");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid " + name + " payload");
        }
    }

    private Map<String, List<String>> getDefaultFilters() {
        Map<String, List<String>> filters = new HashMap<>();
        filters.put("games", List.of("Free Fire", "PUBG", "COD Mobile", "BGMI", "Clash Royale"));
        filters.put("gameModes", List.of("Battle Royale", "Clash Squad", "Lone Wolf"));
        filters.put("teamSizes", List.of("Solo", "Duo", "Squad", "Hexa"));
        filters.put("maps", List.of("Bermuda", "Purgatory", "Kalahari", "Alpine", "NeXTerra"));
        filters.put("timeSlots", List.of("6:00-6:30 PM", "7:00-8:00 PM", "8:00-9:00 PM", "9:00-10:00 PM"));
        return filters;
    }

    private Map<String, Object> getDefaultHelpSupportContent() {
        Map<String, Object> contact = new HashMap<>();
        contact.put("telegramUrl", "https://t.me/Grandbattlearena");
        contact.put("email", "support@grandbattlearena.com");
        contact.put("whatsappUrl", "");

        List<Map<String, String>> faqs = List.of(
                Map.of(
                        "question", "Can I cancel my booking?",
                        "answer", "Bookings cannot be cancelled once registration is complete."),
                Map.of(
                        "question", "How do I deposit money?",
                        "answer",
                        "Go to Wallet > Deposit, complete payment via QR, then submit your UTR for verification."),
                Map.of(
                        "question", "When do I get room credentials?",
                        "answer", "Room ID and password are shared shortly before the tournament start time."));

        Map<String, Object> payload = new HashMap<>();
        payload.put("contact", contact);
        payload.put("faqs", faqs);
        return payload;
    }

    private Map<String, Object> getDefaultTermsContent() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Terms and Conditions");
        payload.put("lastUpdated", "2026-03-05");
        payload.put("body",
                "By using GRAND BATTLE ARENA, you agree to platform rules, fair play policy, and wallet policy. "
                        + "The platform is skill-based esports only. Violations such as cheating, exploit use, or account abuse "
                        + "may result in disqualification, reward cancellation, or account suspension.");
        return payload;
    }
}
