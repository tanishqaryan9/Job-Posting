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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
    public void initialize() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {

            InputStream serviceAccount;
            String serviceAccountJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");

            if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
                // Production — loaded from Render environment variable
                serviceAccount = new ByteArrayInputStream(
                        serviceAccountJson.getBytes(StandardCharsets.UTF_8)
                );
            } else {
                // Local dev — loaded from src/main/resources/
                serviceAccount = getClass().getClassLoader()
                        .getResourceAsStream("firebase-service-account.json");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
        }
    }
}