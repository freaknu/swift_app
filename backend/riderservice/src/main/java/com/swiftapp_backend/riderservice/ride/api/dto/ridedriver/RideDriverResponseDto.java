package com.swiftapp_backend.riderservice.ride.api.dto.ridedriver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class RideDriverResponseDto {
    private Long id;
    private Long driverId;
    private Long authUserId;
    private String driverName;
    private String driverEmail;
    private String driverPhone;
    private String driverGender;
    private String driverProfileImage;
}
