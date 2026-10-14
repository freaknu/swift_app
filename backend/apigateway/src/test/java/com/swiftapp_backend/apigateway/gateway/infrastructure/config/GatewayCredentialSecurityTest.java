package com.swiftapp_backend.apigateway.gateway.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@ActiveProfiles("test")
class GatewayCredentialSecurityTest {
    @Autowired private ApplicationContext context;

    @Test
    void credentialListingRequiresBearerAuthentication() {
        WebTestClient.bindToApplicationContext(context).build()
                .get().uri("/api/v1/user-cred/getCredentials/42")
                .header("x-user-id", "42")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void paymentOrderAndVerificationRequireBearerAuthentication() {
        WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
        for (String action : new String[] {"order", "verify"}) {
            client.post().uri("/api/v1/payments/rides/15/" + action)
                    .header("x-user-id", "42").exchange()
                    .expectStatus().isUnauthorized();
        }
    }
}
