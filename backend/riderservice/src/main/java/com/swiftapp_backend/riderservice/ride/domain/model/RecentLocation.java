package com.swiftapp_backend.riderservice.ride.domain.model;

import java.time.LocalDateTime;

/**
 * A previously used ride destination close to the rider's current pickup.
 */
public record RecentLocation(
        Long locationId,
        double destinationLatitude,
        double destinationLongitude,
        String message,
        LocalDateTime rideDate
) {
}
