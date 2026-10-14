package com.swiftapp_backend.riderservice.ride.api.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class RecentLocationDto {
    private Long locationId;
    private double destinationLatitude;
    private double destinationLongitude;
    private String message;
    private LocalDateTime rideDate;
}
