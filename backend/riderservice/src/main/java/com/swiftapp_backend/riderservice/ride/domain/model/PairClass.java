package com.swiftapp_backend.riderservice.ride.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class PairClass {
    private double latitude;
    private double longitude;
}
