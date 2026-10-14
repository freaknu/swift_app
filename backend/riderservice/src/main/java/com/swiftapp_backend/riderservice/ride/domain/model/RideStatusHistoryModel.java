package com.swiftapp_backend.riderservice.ride.domain.model;

import com.swiftapp_backend.riderservice.ride.domain.enums.ChangedByType;
import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideStatusHistoryModel {

    private Long id;

    private RideStatus rideStatus;

    private Long changedById;

    private ChangedByType changedByType;

    private String remarks;

    private RideModel ride;

    private LocalDateTime createdAt;
}
