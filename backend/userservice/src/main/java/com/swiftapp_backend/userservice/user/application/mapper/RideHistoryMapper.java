package com.swiftapp_backend.userservice.user.application.mapper;


import com.swiftapp_backend.userservice.user.domain.event.RideHistoryCreateEvent;
import com.swiftapp_backend.userservice.user.api.dto.RideHistoryResponseDto;
import com.swiftapp_backend.userservice.user.domain.model.RideHistoryModel;

public class RideHistoryMapper {
    public static RideHistoryModel toModel(RideHistoryCreateEvent data) {
        return RideHistoryModel
                .builder()
                .authUserId(data.getAuthUserId())
                .riderProfileId(data.getRiderId())
                .rideId(data.getRideId())
                .pickUpLatitude(data.getPickUpLatitude())
                .pickUpLongitude(data.getPickUpLongitude())
                .destinationLatitude(data.getDestinationLatitude())
                .destinationLongitude(data.getDestinationLongitude())
                .build();
    }

    public static RideHistoryResponseDto toResponse(RideHistoryModel data) {
        return RideHistoryResponseDto
                .builder()
                .id(data.getId())
                .rideId(data.getRideId())
                .pickUpLatitude(data.getPickUpLatitude())
                .pickUpLongitude(data.getPickUpLongitude())
                .destinationLatitude(data.getDestinationLatitude())
                .destinationLongitude(data.getDestinationLongitude())
                .build();
    }
}
