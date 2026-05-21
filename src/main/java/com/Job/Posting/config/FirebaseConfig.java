package com.Job.Posting.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class FirebaseConfig {

    @PostConstruct
    public void initialize() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FirebaseApp already initialized, skipping.");
            return;
        }

        InputStream serviceAccount = resolveServiceAccount();

        if (serviceAccount == null) {
            log.warn("Firebase credentials not found. " +
                     "Set the FIREBASE_SERVICE_ACCOUNT_JSON environment variable in Render " +
                     "(or add firebase-service-account.json to src/main/resources/ for local dev). " +
                     "Firebase / FCM notifications are DISABLED for this instance.");
            return;
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
        log.info("FirebaseApp initialized successfully.");
    }

    private InputStream resolveServiceAccount() {
        String json = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
        if (json != null && !json.isBlank()) {
            log.info("Loading Firebase credentials from FIREBASE_SERVICE_ACCOUNT_JSON env var.");
            return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        }

        InputStream classpathResource = getClass().getClassLoader()
                .getResourceAsStream("firebase-service-account.json");
        if (classpathResource != null) {
            log.info("Loading Firebase credentials from classpath:firebase-service-account.json.");
        }
        return classpathResource;
    }
}
