package com.swiftapp_backend.websocketservice.realtime.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class NearbyDriver {
    private Long driverId;
    private String nearByPlace;
    private double latitude;
    private double longitude;
}
