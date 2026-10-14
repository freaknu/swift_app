package com.swiftapp_backend.riderservice.ride.domain.repository;

import com.swiftapp_backend.riderservice.ride.domain.model.OfferModel;

import java.util.List;

public interface OfferRepository {
    List<OfferModel> findActiveByRideTypeCode(String rideTypeCode);
}
