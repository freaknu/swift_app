package com.swiftapp_backend.locationservice.eta.application.port.out;

/**
 * Provider-neutral route result. All distances are metres and durations are
 * whole seconds, rounded up when the routing provider returns a fraction.
 * The encoded polyline may be null when the provider did not return one
 * (e.g. a cached entry written before polylines were stored).
 */
public record RouteResult(
        long distanceMeters,
        long durationSeconds,
        long staticDurationSeconds,
        String encodedPolyline
) {
    public RouteResult {
        if (distanceMeters < 0 || durationSeconds < 0 || staticDurationSeconds < 0) {
            throw new IllegalArgumentException("Route values cannot be negative");
        }
    }
}
