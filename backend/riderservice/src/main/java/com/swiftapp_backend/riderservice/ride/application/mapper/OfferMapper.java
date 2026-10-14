package com.swiftapp_backend.riderservice.ride.application.mapper;

import com.swiftapp_backend.riderservice.ride.api.dto.offer.OfferResponseDto;
import com.swiftapp_backend.riderservice.ride.domain.model.OfferModel;

public class OfferMapper {
    public static OfferResponseDto toDto(OfferModel data) {
        return OfferResponseDto
                .builder()
                .id(data.getId())
                .active(data.isActive())
                .offerCode(data.getOfferCode())
                .title(data.getTitle())
                .description(data.getDescription())
                .rideTypeCode(data.getRideTypeCode())
                .discountType(data.getDiscountType())
                .discountValue(data.getDiscountValue())
                .maximumDiscount(data.getMaximumDiscount())
                .minimumFare(data.getMinimumFare())
                .validFrom(data.getValidFrom())
                .validUntil(data.getValidUntil())
                .usageLimit(data.getUsageLimit())
                .perUserLimit(data.getPerUserLimit())
                .usedCount(data.getUsedCount())
                .newUserOnly(data.isNewUserOnly())
                .stackable(data.isStackable())
                .build();
    }
}
