package com.swiftapp_backend.matchingservice.matching.domain.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverOfferAcceptedEvent {

    private Long rideId;

    private Long driverId;

    private Long vehicleId;
}
