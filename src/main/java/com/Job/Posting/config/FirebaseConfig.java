package com.Job.Posting.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class FirebaseConfig {

    // Supports both classpath: and file: prefixes.
    // Default keeps backwards compatibility with the existing classpath resource.
    // In production, override with:
    //   firebase.service-account-path=file:/run/secrets/firebase-service-account.json
    @Value("${firebase.service-account-path:classpath:firebase-service-account.json}")
    private Resource serviceAccountResource;

    @PostConstruct
    public void initialize() {
        // Skip if already initialized (e.g. hot reload)
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase already initialized, skipping.");
            return;
        }

        if (!serviceAccountResource.exists()) {
            log.warn("Firebase service account not found at '{}'. " +
                            "Push notifications will be disabled. " +
                            "This is expected in dev/test environments.",
                    serviceAccountResource.getDescription());
            return;
        }

        try (InputStream stream = serviceAccountResource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully from '{}'.",
                    serviceAccountResource.getDescription());
        } catch (IOException e) {
            log.error("Firebase initialization failed: {}. Push notifications disabled.", e.getMessage());
            // Do NOT rethrow — app must still start without Firebase
        }
    }
}