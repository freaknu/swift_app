package com.swiftapp_backend.userservice.user.application.mapper;

import com.swiftapp_backend.userservice.user.domain.event.ApplyOfferRequestEvent;
import com.swiftapp_backend.userservice.user.domain.model.AppliedOfferTrackModel;

public class ApplyOfferMapper {
    public static AppliedOfferTrackModel toModel(ApplyOfferRequestEvent data) {
        return AppliedOfferTrackModel
                .builder()
                .offerId(data.getOfferId()).authUserId(data.getAuthUserId()).riderId(data.getRiderId()).build();
    }
}
