package com.esport.EsportTournament.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import java.io.FileInputStream;
import java.io.IOException;

public class FirebaseTokenGenerator {
    public static void main(String[] args) throws IOException, FirebaseAuthException {
        FileInputStream serviceAccount = new FileInputStream("/home/time/Desktop/codes/backend/EsportTournament/src/main/resources/firebase-service-account.json");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);

        String uid = "C1YhxuxIPpOgER1boTNu6bSsQfR2"; // your test user UID
        String customToken = FirebaseAuth.getInstance().createCustomToken(uid);
        System.out.println("Custom Token: " + customToken);
    }
}
