package com.swiftapp_backend.riderservice.ride.domain.event;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class RideDriverAcceptResponseEvent {
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
