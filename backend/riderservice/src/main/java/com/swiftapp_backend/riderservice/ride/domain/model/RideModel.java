package com.swiftapp_backend.riderservice.ride.domain.model;

import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import com.swiftapp_backend.riderservice.ride.domain.enums.RideType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideModel {

    private Long id;

    private Long version;

    private Long riderId;

    // Null for terminal rides; uniqueness prevents concurrent active rides per rider.
    private Long activeRiderId;

    private Long driverId;

    // Null for terminal/unassigned rides; uniqueness prevents double-booking a driver.
    private Long activeDriverId;

    private Long vehicleId;

    // Exact selected driver vehicle category (for example HATCHBACK).
    private String rideTypeCode;
    @Builder.Default
    private RideStatus currentStatus = RideStatus.REQUESTED;

    private RideType rideType;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double destinationLatitude;

    private Double destinationLongitude;
    
    // in KM
    private Double estimatedDistance;

    private Double actualDistance;

    @Builder.Default
    private Boolean isCompleted = false;

    // in seconds
    private Long estimatedDuration;

    private Long actualDuration;

    private LocalDateTime requestedAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime driverArrivedAt;

    private LocalDateTime startedAt;

    @Builder.Default
    private Boolean isRideCancelled = false;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private Long cancelledBy;

    private String cancellationReason;

    // applied offer for the ride
    private OfferModel offer;
    private FareDetailModel fareDetail;

    @Builder.Default
    private List<RideStatusHistoryModel> rideHistory = new ArrayList<>();
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
