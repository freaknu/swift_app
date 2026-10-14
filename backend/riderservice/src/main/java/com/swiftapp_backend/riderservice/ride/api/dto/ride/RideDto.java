package com.swiftapp_backend.riderservice.ride.api.dto.ride;

import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareDto;
import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideDto {

    private Long id;

    private Long riderId;
    private Long driverId;
    private Long vehicleId;
    private String rideTypeCode;
    private RideStatus currentStatus;
    private Double pickupLatitude;
    private Double pickupLongitude;

    private Double destinationLatitude;
    private Double destinationLongitude;

    // KM
    private Double estimatedDistance;
    private Double actualDistance;

    private Boolean isCompleted;

    // Seconds
    private Long estimatedDuration;
    private Long actualDuration;

    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime driverArrivedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private Boolean isRideCancelled;
    private LocalDateTime cancelledAt;
    private Long cancelledBy;
    private String cancellationReason;

    private FareDto fareDetail;

    private List<RideStatusHistoryDto> rideHistory;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
