package com.swiftapp_backend.riderservice.ride.infrastructure.client;

import com.swiftapp_backend.riderservice.ride.application.port.MatchingCancellationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class MatchingCancellationHttpClient implements MatchingCancellationPort {
    private final RestClient client;

    public MatchingCancellationHttpClient(
            @Value("${services.matching-url:http://localhost:9005}") String matchingUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        client = RestClient.builder().baseUrl(matchingUrl).requestFactory(factory).build();
    }

    @Override
    public Result cancelPendingRide(Long rideId) {
        Map<?, ?> response = client.post()
                .uri("/api/v1/matching/rides/{rideId}/cancel-pending", rideId)
                .retrieve().body(Map.class);
        Object outcome = response == null ? null : response.get("data");
        if (outcome == null) {
            throw new IllegalStateException("Matching did not confirm cancellation for ride " + rideId);
        }
        return Result.valueOf(outcome.toString());
    }
}
