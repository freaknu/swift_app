package com.swiftapp_backend.websocketservice.realtime.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ChatSessionDto {
    private String sessionNameForDriver;
    private String sessionNameForRider;
    private Long driverId;
    private Long riderId;
    private Long rideId;
    private String rideStatus;
}
