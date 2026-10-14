package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.domain.enums.VehicleTypeCode;
import com.swiftapp_backend.riderservice.ride.domain.model.RideTypesModel;
import org.springframework.stereotype.Service;

import java.util.List;

/** Presents the exact vehicle categories a driver can register and matching can filter. */
@Service
public class RideTypesService {
    public List<RideTypesModel> getRideTypes(Long authUserId, Boolean isDriver) {
        List<VehicleTypeCode> types = VehicleTypeCode.catalog();
        return java.util.stream.IntStream.range(0, types.size())
                .mapToObj(index -> {
                    VehicleTypeCode type = types.get(index);
                    return RideTypesModel.builder()
                            .id((long) index + 1)
                            .code(type.name())
                            .name(type.displayName())
                            .rideType(type.storedRideType())
                            .active(true)
                            .sortOrder(index)
                            .maxPassengers(type.maxPassengers())
                            .build();
                })
                .toList();
    }
}
