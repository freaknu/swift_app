package com.swiftapp_backend.riderservice.ride.domain.model;

import com.swiftapp_backend.riderservice.ride.domain.enums.RideType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideTypesModel {

    private Long id;
    private String code;
    private String name;
    private String description;
    private RideType rideType;
    @Builder.Default
    private boolean active = true;
    @Builder.Default
    private Integer sortOrder = 0;
    @Builder.Default
    private Integer maxPassengers = 1;
    @Builder.Default
    private boolean scheduledSupported = true;
    @Builder.Default
    private boolean cashSupported = true;
    @Builder.Default
    private boolean onlinePaymentSupported = true;
}
