package com.swiftapp_backend.apigateway.gateway.api.controller;

import com.swiftapp_backend.apigateway.gateway.application.service.FallbackService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

class FallbackControllerTest {
    private final WebTestClient client = WebTestClient
            .bindToController(new FallbackController(new FallbackService()))
            .build();

    @Test
    void riderFallbackHandlesForwardedBookingPostAsServiceUnavailable() {
        client.post().uri("/fallback/rider")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.statusCode").isEqualTo(503)
                .jsonPath("$.message").isEqualTo("Ride Service is currently unavailable");
    }

    @Test
    void fallbackSupportsOriginalMutationMethodsForEveryService() {
        for (String path : new String[]{"auth", "user", "location", "driver", "rating",
                "analytics", "matching"}) {
            client.put().uri("/fallback/" + path).exchange().expectStatus().isEqualTo(503);
            client.delete().uri("/fallback/" + path).exchange().expectStatus().isEqualTo(503);
        }
    }
}
