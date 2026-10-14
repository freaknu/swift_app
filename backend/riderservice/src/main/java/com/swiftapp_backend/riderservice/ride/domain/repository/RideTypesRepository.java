package com.swiftapp_backend.riderservice.ride.domain.repository;

import com.swiftapp_backend.riderservice.ride.domain.model.RideTypesModel;

import java.util.List;

public interface RideTypesRepository {
    List<RideTypesModel> findAllBySortOrderAsc();

    List<RideTypesModel> findActiveOrderBySortOrderAsc();
}
