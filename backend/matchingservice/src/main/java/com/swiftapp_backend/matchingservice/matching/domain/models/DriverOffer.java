package com.swiftapp_backend.matchingservice.matching.domain.models;

import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverOfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A single offer sent to one driver for one ride.
 * The status field is the arbiter for the accept/timeout race:
 * only one transition out of PENDING is allowed (atomic Lua CAS).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverOffer {

    private Long rideId;

    private Long driverId;

    private DriverOfferStatus status;

    private int attempt;

    private Instant createdAt;

    private Instant expiresAt;
}
