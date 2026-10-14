package com.swiftapp_backend.websocketservice.realtime.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RiderIdentityClient {
    private final RestClient client;

    public RiderIdentityClient(@Value("${services.user-url:http://localhost:8082}") String userUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        client = RestClient.builder().baseUrl(userUrl).requestFactory(factory).build();
    }

    public boolean belongsTo(Long authUserId, Long riderId) {
        if (authUserId == null || riderId == null || riderId <= 0) return false;
        try {
            JsonNode response = client.get().uri("/api/v1/user/getUserDetails")
                    .header("x-user-id", authUserId.toString())
                    .retrieve().body(JsonNode.class);
            return response != null && riderId.equals(response.path("data").path("id").asLong(-1));
        } catch (RestClientException ex) {
            return false;
        }
    }
}
