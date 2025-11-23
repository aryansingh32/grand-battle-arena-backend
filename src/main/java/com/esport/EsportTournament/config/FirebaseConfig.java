package com.esport.EsportTournament.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initFirebase() throws Exception {
        String credentials = System.getenv("FIREBASE_CREDENTIALS");

        if (credentials == null || credentials.isBlank()) {
            throw new IllegalStateException("FIREBASE_CREDENTIALS env variable is missing!");
        }

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(
                            GoogleCredentials.fromStream(
                                    new ByteArrayInputStream(credentials.getBytes(StandardCharsets.UTF_8))
                            )
                    )
                    .build();

            FirebaseApp.initializeApp(options);
        }
    }
}
