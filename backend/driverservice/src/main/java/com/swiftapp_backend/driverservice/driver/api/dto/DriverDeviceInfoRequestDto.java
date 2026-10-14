package com.swiftapp_backend.driverservice.driver.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverDeviceInfoRequestDto {
    private String deviceToken;
    private String deviceModel;
    private String osVersion;
    private String appVersion;
}
