package com.swiftapp_backend.locationservice.eta.infrastructure.routing.google;

import com.swiftapp_backend.locationservice.eta.application.port.out.RouteResult;
import com.swiftapp_backend.locationservice.eta.application.port.out.RoutingException;
import com.swiftapp_backend.locationservice.eta.infrastructure.routing.google.dto.GoogleRouteRequest;
import com.swiftapp_backend.locationservice.eta.infrastructure.routing.google.dto.GoogleRouteResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class GoogleRoutesMapper {
    private static final Pattern DURATION = Pattern.compile("[0-9]+(?:\\.[0-9]{1,9})?s");

    /**
     * Maps the selected vehicle category to a
     * Google Routes API travel mode. Bike-class rides use TWO_WHEELER;
     * everything else, including unknown or missing values, falls back to DRIVE.
     */
    public static String toTravelMode(String rideType) {
        if (rideType == null) {
            return "DRIVE";
        }
        switch (rideType.trim().toUpperCase(Locale.ROOT)) {
            case "BIKE":
            case "SCOOTER":
            case "ELECTRIC_BIKE":
            case "DELIVERY_BIKE":
                return "TWO_WHEELER";
            default:
                return "DRIVE";
        }
    }

    public GoogleRouteRequest toRequest(double originLatitude, double originLongitude,
                                       double destinationLatitude, double destinationLongitude,
                                       String travelMode) {
        return GoogleRouteRequest.builder()
                .origin(waypoint(originLatitude, originLongitude))
                .destination(waypoint(destinationLatitude, destinationLongitude))
                .travelMode(travelMode)
                .routingPreference("TRAFFIC_AWARE")
                .build();
    }

    public RouteResult toResult(GoogleRouteResponse response) {
        if (response == null) {
            throw invalidResponse();
        }
        // Google may omit an empty repeated field entirely when no route exists.
        if (response.getRoutes() == null || response.getRoutes().isEmpty()) {
            throw new RoutingException(RoutingException.Reason.NO_ROUTE,
                    "No driving route found between origin and destination");
        }
        GoogleRouteResponse.Route route = response.getRoutes().getFirst();
        if (route == null || route.getDistanceMeters() == null || route.getDistanceMeters() < 0) {
            throw invalidResponse();
        }
        String encodedPolyline = route.getPolyline() != null
                ? route.getPolyline().getEncodedPolyline()
                : null;
        return new RouteResult(route.getDistanceMeters(), parseDuration(route.getDuration()),
                parseDuration(route.getStaticDuration()), encodedPolyline);
    }

    private GoogleRouteRequest.Location waypoint(double latitude, double longitude) {
        return new GoogleRouteRequest.Location(new GoogleRouteRequest.LocationWrapper(
                new GoogleRouteRequest.LatLng(latitude, longitude)));
    }

    private long parseDuration(String duration) {
        if (duration == null || !DURATION.matcher(duration).matches()) {
            throw invalidResponse();
        }
        try {
            return new BigDecimal(duration.substring(0, duration.length() - 1))
                    .setScale(0, RoundingMode.CEILING).longValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            throw invalidResponse();
        }
    }

    private RoutingException invalidResponse() {
        return new RoutingException(RoutingException.Reason.INVALID_RESPONSE,
                "Routing provider returned an invalid route response");
    }
}
