package com.swiftapp_backend.websocketservice.realtime.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.swiftapp_backend.websocketservice.realtime.api.dto.ChatSessionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/** A bootstrap request is a hint, never authority to change a ride's participants. */
@Component
public class RideChatSessionClient {
    private static final Set<String> ACTIVE_STATUSES = Set.of("DRIVER_ASSIGNED", "DRIVER_ARRIVED", "STARTED");
    private final RestClient rideClient;
    private final RestClient userClient;
    private final DriverIdentityClient driverIdentityClient;

    @Autowired
    public RideChatSessionClient(
            @Value("${services.rider-url:http://localhost:8083}") String riderUrl,
            @Value("${services.user-url:http://localhost:8082}") String userUrl,
            DriverIdentityClient driverIdentityClient) {
        this(client(riderUrl), client(userUrl), driverIdentityClient);
    }

    RideChatSessionClient(RestClient rideClient, RestClient userClient,
                          DriverIdentityClient driverIdentityClient) {
        this.rideClient = rideClient;
        this.userClient = userClient;
        this.driverIdentityClient = driverIdentityClient;
    }

    public Context validate(Long authUserId, ChatSessionDto request) {
        if (authUserId == null || authUserId <= 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user is required");
        }
        if (request == null || !positive(request.getRideId())
                || !positive(request.getRiderId()) || !positive(request.getDriverId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ride and participant ids are required");
        }
        JsonNode response;
        try {
            response = rideClient.get()
                    .uri("/api/v1/ride/getRideDetail/{rideId}", request.getRideId())
                    .header("x-user-id", authUserId.toString())
                    .retrieve().body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not verify ride for chat");
        }
        JsonNode ride = response == null ? null : response.path("data");
        if (ride == null || !ride.isObject() || ride.path("id").asLong(-1) != request.getRideId()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Invalid ride response");
        }
        String status = ride.path("currentStatus").asText();
        if (!ACTIVE_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chat is not available for this ride status");
        }
        Long riderId = ride.path("riderId").asLong(-1);
        Long driverId = ride.path("driverId").asLong(-1);
        if (!request.getRiderId().equals(riderId) || !request.getDriverId().equals(driverId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chat participants must match the ride");
        }
        if (!belongsToDriver(authUserId, driverId) && !belongsToRider(authUserId, riderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ride participants can bootstrap chat");
        }
        return new Context(request.getRideId(), riderId, driverId, status);
    }

    private boolean belongsToDriver(Long authUserId, Long driverId) {
        try {
            return driverIdentityClient.belongsTo(authUserId, driverId);
        } catch (RestClientException ex) {
            return false;
        }
    }

    private boolean belongsToRider(Long authUserId, Long riderId) {
        try {
            JsonNode response = userClient.get().uri("/api/v1/user/getUserDetails")
                    .header("x-user-id", authUserId.toString()).retrieve().body(JsonNode.class);
            if (response == null) return false;
            JsonNode profile = response.path("data");
            if (profile.has("user")) profile = profile.path("user");
            return riderId.equals(profile.path("id").asLong(-1));
        } catch (RestClientException ex) {
            return false;
        }
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }

    private static RestClient client(String url) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        return RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public record Context(Long rideId, Long riderId, Long driverId, String rideStatus) {}
}
