package com.swiftapp_backend.noticationservice.notification.infrastructure.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.swiftapp_backend.noticationservice.notification.application.port.ProfileContactPort;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class ProfileContactClient implements ProfileContactPort {
    private final RestClient users;
    private final RestClient drivers;

    public ProfileContactClient(
            @Value("${services.user-url:http://localhost:8082}") String userUrl,
            @Value("${services.driver-url:http://localhost:8096}") String driverUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        users = RestClient.builder().baseUrl(userUrl).requestFactory(factory).build();
        drivers = RestClient.builder().baseUrl(driverUrl).requestFactory(factory).build();
    }

    @Override
    public Contact find(Long profileId, UserRole role) {
        if (profileId == null || profileId <= 0 || role == null) return null;
        RestClient client = role == UserRole.DRIVER ? drivers : users;
        String path = role == UserRole.DRIVER
                ? "/internal/profiles/drivers/{id}" : "/internal/profiles/users/{id}";
        try {
            JsonNode response = client.get().uri(path, profileId).retrieve().body(JsonNode.class);
            JsonNode data = response == null ? null : response.path("data");
            if (response == null || !response.path("success").asBoolean()
                    || data == null || data.path("id").asLong(-1) != profileId) return null;
            if (!role.name().equals(data.path("role").asText())) return null;
            return new Contact(profileId, data.path("authUserId").asLong(-1),
                    data.path("fullName").asText(""), data.path("email").asText(""),
                    data.path("role").asText(""),
                    data.hasNonNull("pushNotificationEnabled")
                            ? data.path("pushNotificationEnabled").asBoolean() : null,
                    data.hasNonNull("emailNotificationEnabled")
                            ? data.path("emailNotificationEnabled").asBoolean() : null,
                    data.hasNonNull("rideAlertsEnabled")
                            ? data.path("rideAlertsEnabled").asBoolean() : null);
        } catch (HttpClientErrorException.NotFound ignored) {
            return null; // Deleted or incomplete profiles should not poison the topic.
        }
    }
}
