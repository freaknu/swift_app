package com.swiftapp_backend.websocketservice.realtime.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Resolves a driver record ID from a validated auth user ID. */
@Component
public class DriverIdentityClient {
    private final RestClient client;

    public DriverIdentityClient(@Value("${services.driver-url:http://localhost:8096}") String driverUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(3000);
        client = RestClient.builder().baseUrl(driverUrl)
                .requestFactory(requestFactory).build();
    }

    public boolean belongsTo(Long authUserId, Long driverId) {
        JsonNode response = client.get().uri("/api/v1/driver/profile")
                .header("x-user-id", authUserId.toString())
                .retrieve().body(JsonNode.class);
        return response != null && driverId.equals(
                response.path("data").path("driverId").asLong(-1));
    }
}
