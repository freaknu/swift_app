package com.swiftapp_backend.driverservice.driver.infrastructure.client;

import com.swiftapp_backend.driverservice.driver.application.service.MatchingDecisionUnavailableException;
import com.swiftapp_backend.driverservice.driver.application.port.MatchingDecisionPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** The matching service is the authority on who won an offer. */
@Component
public class MatchingDecisionClient implements MatchingDecisionPort {
    private final RestClient client;

    public MatchingDecisionClient(
            @Value("${services.matching-url:http://localhost:9005}") String matchingUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(3000);
        client = RestClient.builder().baseUrl(matchingUrl)
                .requestFactory(requestFactory).build();
    }

    @Override
    public String accept(Long rideId, Long driverId, Long vehicleId) {
        return decide(rideId, driverId, vehicleId, null, true);
    }

    @Override
    public String reject(Long rideId, Long driverId, String reason) {
        return decide(rideId, driverId, null, reason, false);
    }

    private String decide(Long rideId, Long driverId, Long vehicleId,
                          String reason, boolean accept) {
        try {
            Map<?, ?> response = client.post()
                    .uri(builder -> {
                        builder.path("/api/v1/matching/offers/{rideId}/" +
                                (accept ? "accept" : "reject"));
                        builder.queryParam("driverId", driverId);
                        if (vehicleId != null) builder.queryParam("vehicleId", vehicleId);
                        if (reason != null && !reason.isBlank()) builder.queryParam("reason", reason);
                        return builder.build(rideId);
                    })
                    .retrieve().body(Map.class);
            Object resolution = response == null ? null : response.get("data");
            if (resolution == null) throw new IllegalStateException("Empty matching decision");
            return resolution.toString();
        } catch (Exception ex) {
            throw new MatchingDecisionUnavailableException(
                    "Matching service could not confirm the offer decision", ex);
        }
    }
}
