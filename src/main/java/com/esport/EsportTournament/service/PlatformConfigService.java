package com.esport.EsportTournament.service;

import com.esport.EsportTournament.repository.AppConfigRepo;
import com.esport.EsportTournament.model.AppConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformConfigService {

    private final AppConfigRepo appConfigRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get platform info
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", getConfigValue("platform_name", "ESport Tournament Platform"));
        info.put("version", getConfigValue("platform_version", "1.0.0"));
        info.put("description", getConfigValue("platform_description", "Competitive gaming tournament platform with wallet system"));
        info.put("supportEmail", getConfigValue("platform_support_email", "support@esporttournament.com"));
        info.put("termsUrl", getConfigValue("platform_terms_url", "https://esporttournament.com/terms"));
        info.put("privacyUrl", getConfigValue("platform_privacy_url", "https://esporttournament.com/privacy"));
        return info;
    }

    /**
     * Update platform info
     */
    @Transactional
    public Map<String, Object> updatePlatformInfo(Map<String, Object> info, String adminUID) {
        if (info.containsKey("name")) {
            setConfigValue("platform_name", info.get("name").toString(), adminUID);
        }
        if (info.containsKey("version")) {
            setConfigValue("platform_version", info.get("version").toString(), adminUID);
        }
        if (info.containsKey("description")) {
            setConfigValue("platform_description", info.get("description").toString(), adminUID);
        }
        if (info.containsKey("supportEmail")) {
            setConfigValue("platform_support_email", info.get("supportEmail").toString(), adminUID);
        }
        if (info.containsKey("termsUrl")) {
            setConfigValue("platform_terms_url", info.get("termsUrl").toString(), adminUID);
        }
        if (info.containsKey("privacyUrl")) {
            setConfigValue("platform_privacy_url", info.get("privacyUrl").toString(), adminUID);
        }
        log.info("Admin {} updated platform info", adminUID);
        return getPlatformInfo();
    }

    /**
     * Get registration requirements
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRegistrationRequirements() {
        Map<String, Object> requirements = new HashMap<>();
        requirements.put("minimumAge", Integer.parseInt(getConfigValue("registration_minimum_age", "13")));
        
        String requiredDocsJson = getConfigValue("registration_required_documents", null);
        if (requiredDocsJson != null) {
            try {
                requirements.put("requiredDocuments", objectMapper.readValue(requiredDocsJson, new TypeReference<List<String>>() {}));
            } catch (Exception e) {
                requirements.put("requiredDocuments", List.of("Valid email address", "Firebase authentication", "Unique username"));
            }
        } else {
            requirements.put("requiredDocuments", List.of("Valid email address", "Firebase authentication", "Unique username"));
        }
        
        requirements.put("termsAndConditions", getConfigValue("registration_terms", "Must be 13+ years old. One account per person."));
        requirements.put("privacyPolicy", getConfigValue("registration_privacy", "Your data is protected and will not be shared with third parties."));
        return requirements;
    }

    /**
     * Update registration requirements
     */
    @Transactional
    public Map<String, Object> updateRegistrationRequirements(Map<String, Object> requirements, String adminUID) {
        if (requirements.containsKey("minimumAge")) {
            setConfigValue("registration_minimum_age", requirements.get("minimumAge").toString(), adminUID);
        }
        if (requirements.containsKey("requiredDocuments")) {
            try {
                @SuppressWarnings("unchecked")
                List<String> docs = (List<String>) requirements.get("requiredDocuments");
                String docsJson = objectMapper.writeValueAsString(docs);
                setConfigValue("registration_required_documents", docsJson, adminUID);
            } catch (Exception e) {
                log.error("Error saving required documents: {}", e.getMessage());
            }
        }
        if (requirements.containsKey("termsAndConditions")) {
            setConfigValue("registration_terms", requirements.get("termsAndConditions").toString(), adminUID);
        }
        if (requirements.containsKey("privacyPolicy")) {
            setConfigValue("registration_privacy", requirements.get("privacyPolicy").toString(), adminUID);
        }
        log.info("Admin {} updated registration requirements", adminUID);
        return getRegistrationRequirements();
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
}

