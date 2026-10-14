package com.swiftapp_backend.websocketservice.realtime.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Checks that a ride topic subscriber owns the rider or assigned driver profile. */
@Component
public class RideSubscriptionAccessClient {
    private final RestClient client;
    private final RiderIdentityClient riderIdentity;
    private final DriverIdentityClient driverIdentity;

    public RideSubscriptionAccessClient(
            @Value("${services.rider-url:http://localhost:8083}") String riderUrl,
            RiderIdentityClient riderIdentity,
            DriverIdentityClient driverIdentity) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        client = RestClient.builder().baseUrl(riderUrl).requestFactory(factory).build();
        this.riderIdentity = riderIdentity;
        this.driverIdentity = driverIdentity;
    }

    public boolean belongsTo(Long authUserId, Long rideId) {
        if (authUserId == null || rideId == null || rideId <= 0) return false;
        try {
            JsonNode response = client.get().uri("/internal/rides/{rideId}", rideId)
                    .retrieve().body(JsonNode.class);
            JsonNode ride = response == null ? null : response.path("data");
            if (ride == null || ride.path("id").asLong(-1) != rideId) return false;
            Long riderId = ride.path("riderId").asLong(-1);
            Long driverId = ride.path("driverId").asLong(-1);
            return riderIdentity.belongsTo(authUserId, riderId)
                    || driverIdentity.belongsTo(authUserId, driverId);
        } catch (RestClientException ex) {
            return false;
        }
    }
}
