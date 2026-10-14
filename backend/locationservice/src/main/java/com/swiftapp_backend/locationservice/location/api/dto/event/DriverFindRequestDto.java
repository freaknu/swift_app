package com.swiftapp_backend.locationservice.location.api.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverFindRequestDto {
    private double latitude;
    private double longitude;
}
