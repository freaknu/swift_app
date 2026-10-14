package com.swiftapp_backend.riderservice.ride.application.mapper;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideStatusHistoryDto;
import com.swiftapp_backend.riderservice.ride.domain.model.RideStatusHistoryModel;
import org.springframework.stereotype.Component;

@Component
public class RideHistoryMapper {

    public RideStatusHistoryDto toDto(RideStatusHistoryModel model) {

        if (model == null) {
            return null;
        }

        return RideStatusHistoryDto.builder()
                .id(model.getId())
                .rideStatus(model.getRideStatus())
                .changedById(model.getChangedById())
                .changedByType(model.getChangedByType())
                .remarks(model.getRemarks())
                .createdAt(model.getCreatedAt())
                .build();
    }

    public RideStatusHistoryModel toModel(RideStatusHistoryDto dto) {

        if (dto == null) {
            return null;
        }

        return RideStatusHistoryModel.builder()
                .id(dto.getId())
                .rideStatus(dto.getRideStatus())
                .changedById(dto.getChangedById())
                .changedByType(dto.getChangedByType())
                .remarks(dto.getRemarks())
                .createdAt(dto.getCreatedAt())
                .build();
    }
}