package com.swiftapp_backend.locationservice.location.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Resolves the authenticated driver's current availability and vehicle. */
@Component
public class DriverPresenceClient {
    private final RestClient client;

    public DriverPresenceClient(@Value("${services.driver-url:http://localhost:8096}") String driverUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.client = RestClient.builder().baseUrl(driverUrl).requestFactory(factory).build();
    }

    public Presence get(Long authUserId, Long expectedDriverId) {
        JsonNode response = client.get()
                .uri("/api/v1/driver/profile")
                .header("x-user-id", authUserId.toString())
                .retrieve()
                .body(JsonNode.class);
        JsonNode profile = response == null ? null : response.path("data");
        if (profile == null || profile.isMissingNode() || profile.isNull()
                || !expectedDriverId.equals(profile.path("driverId").asLong(-1))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Location driverId does not belong to this account");
        }

        String vehicleType = null;
        JsonNode vehicles = profile.path("allVehicles");
        if (vehicles.isArray()) {
            for (JsonNode vehicle : vehicles) {
                if ((vehicle.path("isActive").asBoolean(false)
                        || vehicle.path("active").asBoolean(false))
                        && vehicle.hasNonNull("vehicleType")) {
                    vehicleType = vehicle.get("vehicleType").asText();
                    break;
                }
            }
        }
        boolean online = "ONLINE".equals(profile.path("driverStatus").asText())
                && (profile.path("isActive").asBoolean(false)
                    || profile.path("active").asBoolean(false));
        return new Presence(online ? "ACTIVE" : "INACTIVE", vehicleType);
    }

    public record Presence(String driverStatus, String vehicleTypeEnum) {}
}
