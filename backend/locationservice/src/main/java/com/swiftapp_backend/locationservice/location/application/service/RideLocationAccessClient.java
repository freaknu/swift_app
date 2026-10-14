package com.swiftapp_backend.locationservice.location.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Resolves ride assignment and participant identity from the owning services. */
@Component
public class RideLocationAccessClient {
    private final RestClient rides;
    private final RestClient users;
    private final RestClient drivers;

    public RideLocationAccessClient(
            @Value("${services.rider-url:http://localhost:8083}") String riderUrl,
            @Value("${services.user-url:http://localhost:8082}") String userUrl,
            @Value("${services.driver-url:http://localhost:8096}") String driverUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        rides = RestClient.builder().baseUrl(riderUrl).requestFactory(factory).build();
        users = RestClient.builder().baseUrl(userUrl).requestFactory(factory).build();
        drivers = RestClient.builder().baseUrl(driverUrl).requestFactory(factory).build();
    }

    public Ride assignedToDriver(Long rideId, Long driverId) {
        Ride ride = getRide(rideId);
        if (!driverId.equals(ride.driverId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver is not assigned to this ride");
        }
        if (!switch (ride.status()) {
            case "DRIVER_ASSIGNED", "DRIVER_ARRIVED", "STARTED" -> true;
            default -> false;
        }) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride is not active");
        }
        return ride;
    }

    public Ride participant(Long rideId, Long expectedDriverId, Long authUserId) {
        Ride ride = getRide(rideId);
        if (expectedDriverId != null && !expectedDriverId.equals(ride.driverId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver is not assigned to this ride");
        }
        if (authUserId == null || authUserId <= 0
                || !(isOwner(drivers, "/internal/profiles/drivers/{id}", ride.driverId(), authUserId)
                || isOwner(users, "/internal/profiles/users/{id}", ride.riderId(), authUserId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ride location is private to its participants");
        }
        return ride;
    }

    private Ride getRide(Long rideId) {
        if (rideId == null || rideId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ride id is required");
        }
        try {
            JsonNode response = rides.get().uri("/internal/rides/{id}", rideId)
                    .retrieve().body(JsonNode.class);
            JsonNode data = response == null ? null : response.path("data");
            if (response == null || !response.path("success").asBoolean()
                    || data == null || data.path("id").asLong(-1) != rideId) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found");
            }
            return new Ride(rideId, nullableLong(data, "riderId"),
                    nullableLong(data, "driverId"), data.path("currentStatus").asText(""),
                    nullableDouble(data, "pickupLatitude"), nullableDouble(data, "pickupLongitude"),
                    nullableDouble(data, "destinationLatitude"), nullableDouble(data, "destinationLongitude"));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Ride assignment could not be verified", ex);
        }
    }

    private boolean isOwner(RestClient client, String path, Long profileId, Long authUserId) {
        if (profileId == null) return false;
        try {
            JsonNode response = client.get().uri(path, profileId).retrieve().body(JsonNode.class);
            JsonNode profile = response == null ? null : response.path("data");
            return response != null && response.path("success").asBoolean()
                    && profile != null && profile.path("id").asLong(-1) == profileId
                    && profile.path("authUserId").asLong(-1) == authUserId;
        } catch (RestClientException ex) {
            // An unavailable identity service must never grant access.
            return false;
        }
    }

    private Long nullableLong(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asLong() : null;
    }

    private Double nullableDouble(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asDouble() : null;
    }

    public record Ride(Long id, Long riderId, Long driverId, String status,
                       Double pickupLatitude, Double pickupLongitude,
                       Double destinationLatitude, Double destinationLongitude) {
        public Ride(Long id, Long riderId, Long driverId, String status) {
            this(id, riderId, driverId, status, null, null, null, null);
        }
    }
}
