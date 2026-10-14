package com.swiftapp_backend.locationservice.eta.infrastructure.routing.google;

import com.swiftapp_backend.locationservice.eta.application.port.out.RoutingException;
import com.swiftapp_backend.locationservice.eta.infrastructure.routing.google.dto.GoogleRouteRequest;
import com.swiftapp_backend.locationservice.eta.infrastructure.routing.google.dto.GoogleRouteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class GoogleRoutesClient {
    private static final String FIELD_MASK =
            "routes.distanceMeters,routes.duration,routes.staticDuration,routes.polyline.encodedPolyline";

    private final RestClient restClient;

    @Value("${google.maps.api-key:}")
    private String apiKey;

    public GoogleRouteResponse calculateRoute(GoogleRouteRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RoutingException(RoutingException.Reason.UNAVAILABLE,
                    "Google Maps API key is not configured");
        }

        try {
            return restClient.post()
                    .uri("/directions/v2:computeRoutes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .body(request)
                    .retrieve()
                    .body(GoogleRouteResponse.class);
        } catch (ResourceAccessException ex) {
            throw new RoutingException(RoutingException.Reason.TIMEOUT,
                    "Google Routes API is unavailable");
        } catch (RestClientResponseException ex) {
            throw new RoutingException(RoutingException.Reason.UNAVAILABLE,
                    "Google Routes API rejected the route request");
        } catch (RestClientException ex) {
            throw new RoutingException(RoutingException.Reason.UNAVAILABLE,
                    "Google Routes API is unavailable");
        }
    }
}
