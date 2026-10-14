package com.swiftapp_backend.riderservice.ride.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.AssignedRiderSummaryDto;
import com.swiftapp_backend.riderservice.ride.application.port.RiderRideSummaryPort;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RiderRideSummaryHttpClient implements RiderRideSummaryPort {
    private final RestClient client;

    public RiderRideSummaryHttpClient(
            @Value("${services.user-url:http://localhost:8082}") String userUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        client = RestClient.builder().baseUrl(userUrl).requestFactory(factory).build();
    }

    @Override
    public AssignedRiderSummaryDto get(Long riderId) {
        try {
            JsonNode response = client.get().uri("/internal/profiles/users/{id}", riderId)
                    .retrieve().body(JsonNode.class);
            JsonNode data = response == null ? null : response.path("data");
            if (response == null || !response.path("success").asBoolean()
                    || data == null || data.path("id").asLong(-1) != riderId) {
                throw ApiException.notFound("Assigned rider details unavailable");
            }
            return new AssignedRiderSummaryDto(riderId,
                    data.hasNonNull("fullName") ? data.path("fullName").asText() : null);
        } catch (HttpClientErrorException.NotFound ex) {
            throw ApiException.notFound("Assigned rider details unavailable");
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Rider details are temporarily unavailable");
        }
    }
}
