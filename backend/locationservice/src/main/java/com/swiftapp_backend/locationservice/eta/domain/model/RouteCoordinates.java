package com.swiftapp_backend.locationservice.eta.domain.model;

public record RouteCoordinates(
        double originLatitude,
        double originLongitude,
        double destinationLatitude,
        double destinationLongitude
) {
    public RouteCoordinates {
        validate(originLatitude, 90, "Origin latitude");
        validate(originLongitude, 180, "Origin longitude");
        validate(destinationLatitude, 90, "Destination latitude");
        validate(destinationLongitude, 180, "Destination longitude");
    }

    private static void validate(double value, int limit, String name) {
        if (!Double.isFinite(value) || Math.abs(value) > limit) {
            throw new IllegalArgumentException(name + " must be between -" + limit + " and " + limit);
        }
    }
}
