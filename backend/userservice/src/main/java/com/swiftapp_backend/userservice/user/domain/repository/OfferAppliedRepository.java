package com.swiftapp_backend.userservice.user.domain.repository;

import com.swiftapp_backend.userservice.user.domain.model.AppliedOfferTrackModel;

import java.util.List;

public interface OfferAppliedRepository {
    AppliedOfferTrackModel save(AppliedOfferTrackModel offer);

    List<AppliedOfferTrackModel> findAllByAuthUserId(Long authUserId);

    List<AppliedOfferTrackModel> findAllByOfferId(Long offerId);

    List<AppliedOfferTrackModel> findAllByOfferIdAndAuthUserId(Long offerId,Long authUserId);

    Integer countByOfferIdAndAuthUserId(Long offerId,Long authUserId);
}
