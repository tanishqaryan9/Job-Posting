package com.Job.Posting.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        // Skip if already initialized (e.g. hot reload)
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase already initialized, skipping.");
            return;
        }

        InputStream serviceAccount =
                getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");

        if (serviceAccount == null) {
            log.warn("firebase-service-account.json not found. " +
                     "Push notifications will be disabled. " +
                     "This is expected in dev/test environments.");
            return; // App continues to start — Firebase is optional
        }

        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully.");
        } catch (IOException e) {
            log.error("Firebase initialization failed: {}. Push notifications disabled.", e.getMessage());
            // Do NOT rethrow — app must still start without Firebase
        }
    }
}
