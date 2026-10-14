package com.swiftapp_backend.riderservice.ride.api.dto.ride;

import com.swiftapp_backend.riderservice.ride.domain.enums.ChangedByType;
import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideStatusHistoryDto {

    private Long id;

    private RideStatus rideStatus;

    private Long changedById;

    private ChangedByType changedByType;

    private String remarks;

    private LocalDateTime createdAt;
}