package com.swiftapp_backend.riderservice.ride.api.dto.offer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class OfferFetchRequestDto {
    private String rideTypeCode;
    private LocalDateTime startFrom;
}
