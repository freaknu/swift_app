package com.swiftapp_backend.riderservice.ride.api.dto.driver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverDto {
    private Long id;
    private Long driverId;
    private Long driverName;
    
}
