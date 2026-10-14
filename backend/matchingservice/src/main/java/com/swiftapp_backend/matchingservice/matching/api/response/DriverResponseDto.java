package com.swiftapp_backend.matchingservice.matching.api.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverResponseDto {
    private Long driverId;
    private double latitude;
    private double longitude;
}
