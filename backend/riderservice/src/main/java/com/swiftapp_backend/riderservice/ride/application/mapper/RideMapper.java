package com.swiftapp_backend.riderservice.ride.application.mapper;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideDto;
import com.swiftapp_backend.riderservice.ride.domain.model.RideModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RideMapper {

    private final FareMapper fareMapper;
    private final RideHistoryMapper rideHistoryMapper;

    public RideDto toDto(RideModel model) {

        if (model == null) {
            return null;
        }

        return RideDto.builder()
                .id(model.getId())
                .riderId(model.getRiderId())
                .driverId(model.getDriverId())
                .vehicleId(model.getVehicleId())
                .rideTypeCode(model.getRideTypeCode())
                .currentStatus(model.getCurrentStatus())
                .pickupLatitude(model.getPickupLatitude())
                .pickupLongitude(model.getPickupLongitude())
                .destinationLatitude(model.getDestinationLatitude())
                .destinationLongitude(model.getDestinationLongitude())
                .estimatedDistance(model.getEstimatedDistance())
                .actualDistance(model.getActualDistance())
                .isCompleted(model.getIsCompleted())
                .estimatedDuration(model.getEstimatedDuration())
                .actualDuration(model.getActualDuration())
                .requestedAt(model.getRequestedAt())
                .acceptedAt(model.getAcceptedAt())
                .driverArrivedAt(model.getDriverArrivedAt())
                .startedAt(model.getStartedAt())
                .completedAt(model.getCompletedAt())
                .isRideCancelled(model.getIsRideCancelled())
                .cancelledAt(model.getCancelledAt())
                .cancelledBy(model.getCancelledBy())
                .cancellationReason(model.getCancellationReason())
                .fareDetail(fareMapper.toDto(model.getFareDetail()))
                .rideHistory(
                        model.getRideHistory() == null ? null :
                                model.getRideHistory()
                                        .stream()
                                        .map(rideHistoryMapper::toDto)
                                        .collect(Collectors.toList())
                )
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    public RideModel toModel(RideDto dto) {

        if (dto == null) {
            return null;
        }

        return RideModel.builder()
                .id(dto.getId())
                .riderId(dto.getRiderId())
                .driverId(dto.getDriverId())
                .vehicleId(dto.getVehicleId())
                .rideTypeCode(dto.getRideTypeCode())
                .currentStatus(dto.getCurrentStatus())
                .pickupLatitude(dto.getPickupLatitude())
                .pickupLongitude(dto.getPickupLongitude())
                .destinationLatitude(dto.getDestinationLatitude())
                .destinationLongitude(dto.getDestinationLongitude())
                .estimatedDistance(dto.getEstimatedDistance())
                .actualDistance(dto.getActualDistance())
                .isCompleted(dto.getIsCompleted())
                .estimatedDuration(dto.getEstimatedDuration())
                .actualDuration(dto.getActualDuration())
                .requestedAt(dto.getRequestedAt())
                .acceptedAt(dto.getAcceptedAt())
                .driverArrivedAt(dto.getDriverArrivedAt())
                .startedAt(dto.getStartedAt())
                .completedAt(dto.getCompletedAt())
                .isRideCancelled(dto.getIsRideCancelled())
                .cancelledAt(dto.getCancelledAt())
                .cancelledBy(dto.getCancelledBy())
                .cancellationReason(dto.getCancellationReason())
                .fareDetail(fareMapper.toModel(dto.getFareDetail()))
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
