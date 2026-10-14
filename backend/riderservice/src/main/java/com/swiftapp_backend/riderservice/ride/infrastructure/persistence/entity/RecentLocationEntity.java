package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity;

import java.time.LocalDateTime;

/**
 * Infrastructure representation of one row returned by the native nearby
 * destination query. It is mapped to the domain read model by the adapter.
 */
public record RecentLocationEntity(
        Long locationId,
        double destinationLatitude,
        double destinationLongitude,
        String message,
        LocalDateTime rideDate
) {
}
