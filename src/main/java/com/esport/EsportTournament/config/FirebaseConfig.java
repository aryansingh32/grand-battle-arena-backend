package com.esport.EsportTournament.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@Lazy(false) // Force eager initialization - CRITICAL!
public class FirebaseConfig {

    @PostConstruct
    public void initFirebase() {
        try {
            log.info("🔥 Starting Firebase initialization...");

            String credentials = System.getenv("FIREBASE_CREDENTIALS");
            String credentialsBase64 = System.getenv("FIREBASE_SERVICE_ACCOUNT_BASE64");

            // Try base64 version first if available
            if (credentialsBase64 != null && !credentialsBase64.isBlank()) {
                log.info("📦 Using FIREBASE_SERVICE_ACCOUNT_BASE64");
                try {
                    credentials = new String(java.util.Base64.getDecoder().decode(credentialsBase64),
                            StandardCharsets.UTF_8);
                    log.info("✅ Successfully decoded base64 credentials");
                } catch (Exception e) {
                    log.error("❌ Failed to decode base64 credentials: {}", e.getMessage());
                }
            }

            if (credentials == null || credentials.isBlank()) {
                log.error("❌ No Firebase credentials found in environment variables!");
                throw new IllegalStateException("Firebase credentials missing!");
            }

            log.info("✅ Credentials loaded (length: {} chars)", credentials.length());

            // Sanitize private key (replace literal \n with actual newlines)
            if (credentials.contains("\\n")) {
                log.info("🔧 Sanitizing private key in credentials...");
                credentials = credentials.replace("\\\\n", "\\n"); // Handle double escapes first
                credentials = credentials.replace("\\n", "\n");
            }

            if (FirebaseApp.getApps().isEmpty()) {
                log.info("📦 Initializing Firebase app...");

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(
                                GoogleCredentials.fromStream(
                                        new ByteArrayInputStream(credentials.getBytes(StandardCharsets.UTF_8))))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase app initialized successfully!");

                // Verify initialization
                FirebaseApp app = FirebaseApp.getInstance();
                log.info("✅ Firebase app name: {}", app.getName());
                log.info("✅ Firebase project ID: {}", app.getOptions().getProjectId());
            } else {
                log.info("ℹ️  Firebase app already initialized");
            }

        } catch (Exception e) {
            log.error("💥 Failed to initialize Firebase", e);
            log.error("💥 Error type: {}", e.getClass().getName());
            log.error("💥 Error message: {}", e.getMessage());
            // Don't throw exception - let app start but log error clearly
            log.error("⚠️  ⚠️  ⚠️  APPLICATION WILL RUN BUT FIREBASE AUTH WILL NOT WORK! ⚠️  ⚠️  ⚠️ ");
        }
    }
}
