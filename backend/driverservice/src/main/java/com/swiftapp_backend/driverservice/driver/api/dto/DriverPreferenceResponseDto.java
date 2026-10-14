package com.swiftapp_backend.driverservice.driver.api.dto;

import com.swiftapp_backend.driverservice.driver.domain.enums.RideType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverPreferenceResponseDto {
    private Long id;
    private boolean autoAccept;
    private boolean acceptCash;
    private boolean acceptOnline;
    private double preferredRadius;
    private Set<RideType> preferredRideTypes;
}