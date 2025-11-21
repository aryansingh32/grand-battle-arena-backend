package com.esport.EsportTournament.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.config-path:firebase-service-account.json}")
    private String firebaseConfigPath;

    @Value("${FIREBASE_SERVICE_ACCOUNT_BASE64:}")
    private String firebaseServiceAccountBase64;

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount;

                // Priority 1: Use base64 encoded credentials from environment (Railway)
                if (firebaseServiceAccountBase64 != null && !firebaseServiceAccountBase64.isEmpty()) {
                    log.info("Initializing Firebase from base64 encoded credentials");
                    byte[] decodedBytes = Base64.getDecoder().decode(firebaseServiceAccountBase64);
                    serviceAccount = new ByteArrayInputStream(decodedBytes);
                } else {
                    // Priority 2: Use file from classpath
                    log.info("Initializing Firebase from file: {}", firebaseConfigPath);
                    serviceAccount = new ClassPathResource(firebaseConfigPath).getInputStream();
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase Admin SDK initialized successfully");
            } else {
                log.info("Firebase Admin SDK already initialized");
            }
        } catch (IOException e) {
            log.error("❌ Failed to initialize Firebase Admin SDK", e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Return existing instance or create new one
        return FirebaseApp.getApps().isEmpty() ?
                initializeFirebaseApp() : FirebaseApp.getInstance();
    }

    private FirebaseApp initializeFirebaseApp() throws IOException {
        InputStream serviceAccount = new ClassPathResource(firebaseConfigPath).getInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        return FirebaseApp.initializeApp(options);
    }
}