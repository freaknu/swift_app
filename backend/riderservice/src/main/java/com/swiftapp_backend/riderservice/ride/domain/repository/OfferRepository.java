package com.swiftapp_backend.riderservice.ride.domain.repository;

import com.swiftapp_backend.riderservice.ride.domain.model.OfferModel;

import java.util.List;
import java.util.Optional;

public interface OfferRepository {
    List<OfferModel> findActive();
    Optional<OfferModel> findById(Long offerId);
    Optional<OfferModel> findByIdForUpdate(Long offerId);
    OfferModel save(OfferModel offer);
    long countUses(Long offerId, Long riderId);
    boolean hasCompletedRide(Long riderId);
}
