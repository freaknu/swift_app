package com.swiftapp_backend.riderservice.ride.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.AssignedDriverSummaryDto;
import com.swiftapp_backend.riderservice.ride.application.port.DriverRideSummaryPort;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DriverRideSummaryHttpClient implements DriverRideSummaryPort {
    private final RestClient client;

    public DriverRideSummaryHttpClient(
            @Value("${services.driver-url:http://localhost:8096}") String driverUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        client = RestClient.builder().baseUrl(driverUrl).requestFactory(factory).build();
    }

    @Override
    public AssignedDriverSummaryDto get(Long driverId, Long vehicleId) {
        try {
            JsonNode response = client.get().uri(
                    "/internal/profiles/drivers/{driverId}/vehicles/{vehicleId}/ride-summary",
                    driverId, vehicleId).retrieve().body(JsonNode.class);
            JsonNode data = response == null ? null : response.path("data");
            if (response == null || !response.path("success").asBoolean() || data == null
                    || data.path("driverId").asLong(-1) != driverId
                    || data.path("vehicleId").asLong(-1) != vehicleId) {
                throw ApiException.notFound("Assigned driver details unavailable");
            }
            return new AssignedDriverSummaryDto(driverId, text(data, "driverName"),
                    text(data, "profileImage"), vehicleId, text(data, "vehicleType"),
                    text(data, "brand"), text(data, "model"), text(data, "color"),
                    text(data, "registrationNumber"),
                    data.hasNonNull("seatCapacity") ? data.path("seatCapacity").asInt() : null,
                    data.hasNonNull("vehicleVerified") ? data.path("vehicleVerified").asBoolean() : null);
        } catch (HttpClientErrorException.NotFound ex) {
            throw ApiException.notFound("Assigned driver details unavailable");
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Driver details are temporarily unavailable");
        }
    }

    private String text(JsonNode data, String field) {
        return data.hasNonNull(field) ? data.path(field).asText() : null;
    }
}
