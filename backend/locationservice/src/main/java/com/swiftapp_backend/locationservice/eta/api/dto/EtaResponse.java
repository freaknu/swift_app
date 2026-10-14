package com.swiftapp_backend.locationservice.eta.api.dto;

/**
 * Transport representation of a calculated ETA. Google-specific response
 * fields never cross into the API or application contract.
 * The polyline is the Google encoded polyline of the route, or null when
 * the routing provider did not return one.
 */
public record EtaResponse(
        String rideId,
        String driverId,
        double distanceKm,
        long durationSeconds,
        long etaMinutes,
        String polyline
) {
}
