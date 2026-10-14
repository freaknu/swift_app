package com.swiftapp_backend.locationservice.eta.domain.model;

/**
 * Provider-neutral ETA result used inside the ETA use case.
 * The encoded polyline may be null when the routing provider did not
 * return one.
 */
public record Eta(
        String rideId,
        String driverId,
        double distanceKm,
        long durationSeconds,
        long etaMinutes,
        String encodedPolyline
) {
    public Eta {
        if (distanceKm < 0 || durationSeconds < 0 || etaMinutes < 0) {
            throw new IllegalArgumentException("ETA values cannot be negative");
        }
    }
}
