package com.swiftapp_backend.driverservice.driver.api.dto;

import com.swiftapp_backend.driverservice.driver.domain.enums.DriverStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverSmallDetailDto {
    private Long driverId;
    private DriverStatus driverStatus;
    private boolean isVerified = false;
}
