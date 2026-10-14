package com.swiftapp_backend.driverservice.driver.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverPreferenceCreateRequestDto {
    private Boolean autoAccept;
    private Boolean acceptCash;
    private Boolean acceptOnline;
    private Double preferredRadius;
    private Long driverId;
}
