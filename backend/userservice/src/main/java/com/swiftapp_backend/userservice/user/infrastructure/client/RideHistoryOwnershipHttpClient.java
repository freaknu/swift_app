package com.swiftapp_backend.userservice.user.infrastructure.client;

import com.swiftapp_backend.userservice.shared.ApiException;
import com.swiftapp_backend.userservice.user.application.port.RideHistoryOwnershipPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

@Component
public class RideHistoryOwnershipHttpClient implements RideHistoryOwnershipPort {
    private final RestClient rideClient;

    public RideHistoryOwnershipHttpClient(
            @Value("${services.rider-url:${services.ride-url:http://localhost:8083}}") String riderUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        rideClient = RestClient.builder().baseUrl(riderUrl).requestFactory(factory).build();
    }

    @Override
    public Optional<Long> riderProfileIdForRide(Long rideId) {
        try {
            Map<?, ?> response = rideClient.get().uri("/internal/rides/{rideId}", rideId)
                    .retrieve().body(Map.class);
            if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
                throw unavailable();
            }
            Object raw = data.get("riderId");
            if (raw instanceof Number number) return Optional.of(number.longValue());
            if (raw instanceof String value && !value.isBlank()) {
                return Optional.of(Long.valueOf(value));
            }
            throw unavailable();
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException | NumberFormatException ex) {
            throw unavailable();
        }
    }

    private static ApiException unavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "Ride history ownership could not be verified. Please try again.");
    }
}
