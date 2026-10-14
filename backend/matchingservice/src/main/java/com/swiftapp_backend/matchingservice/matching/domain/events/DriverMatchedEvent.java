package com.swiftapp_backend.matchingservice.matching.domain.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Published on {@code driver.matched} when a driver accepts an offer.
 * Shape mirrors riderservice's RideDriverAcceptResponseEvent so its
 * consumer can convert it directly; unknown driver details stay null.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverMatchedEvent {

    private Long rideId;

    private Long driverId;

    private Long vehicleId;

    private Long authUserId;

    private String driverName;

    private String driverEmail;

    private String driverPhone;

    private String driverGender;

    private String driverProfileImage;

    private Boolean isActive;
}
