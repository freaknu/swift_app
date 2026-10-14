package com.swiftapp_backend.noticationservice.notification.infrastructure.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.swiftapp_backend.noticationservice.notification.application.port.RecipientProfilePort;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class RecipientProfileClient implements RecipientProfilePort {
    private final RestTemplate profileRestTemplate;

    @Override
    public Long profileId(Long authUserId, UserRole role) {
        if (authUserId == null || authUserId <= 0 || role == null) {
            throw new IllegalArgumentException("Authenticated user and role are required");
        }
        String url = role == UserRole.DRIVER
                ? "http://DRIVER-SERVICE/api/v1/driver/profile"
                : "http://USER-SERVICE/api/v1/user/getUserDetails";
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-user-id", authUserId.toString());
        JsonNode response = profileRestTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class).getBody();
        long recipient = response == null ? 0 : response.path("data")
                .path(role == UserRole.DRIVER ? "driverId" : "id").asLong();
        if (response == null || !response.path("success").asBoolean() || recipient <= 0) {
            throw new IllegalArgumentException("Create your profile before enabling notifications");
        }
        return recipient;
    }
}
