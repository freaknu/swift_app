package com.swiftapp_backend.noticationservice.notification.infrastructure.config;

import java.io.IOException;
import java.io.InputStream;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.Resource;

@Configuration
@ConditionalOnProperty(name = "notifications.push.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {
    @Bean(destroyMethod = "delete")
    public FirebaseApp firebaseApp(ResourceLoader loader,
            @Value("${notifications.push.credentials:}") String location) throws IOException {
        GoogleCredentials credentials;
        if (location.isBlank()) {
            credentials = GoogleCredentials.getApplicationDefault();
        } else {
            Resource resource = loader.getResource(location);
            if (!resource.exists()) {
                resource = loader.getResource(
                        "file:src/main/resources/static/notification/swift-app-service-account.json");
            }
            if (!resource.exists()) {
                // Packaged jar: the JSON ships under BOOT-INF/classes, so the
                // service starts from any working directory without the
                // relative file: fallbacks above.
                resource = loader.getResource(
                        "classpath:static/notification/swift-app-service-account.json");
            }
            if (!resource.exists()) {
                throw new IOException("Firebase credentials were not found at " + location);
            }
            try (InputStream fs = resource.getInputStream()) {
                credentials = GoogleCredentials.fromStream(fs);
            }
        }
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        return FirebaseApp.initializeApp(options, "swift-notifications");
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }
}
