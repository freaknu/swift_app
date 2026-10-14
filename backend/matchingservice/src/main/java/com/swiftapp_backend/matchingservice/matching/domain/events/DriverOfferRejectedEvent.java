package com.swiftapp_backend.matchingservice.matching.domain.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverOfferRejectedEvent {

    private Long rideId;

    private Long driverId;

    private String reason;
}
