package com.swiftapp_backend.riderservice.ride.application.mapper;

import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareDto;
import com.swiftapp_backend.riderservice.ride.domain.model.FareDetailModel;
import org.springframework.stereotype.Component;

@Component
public class FareMapper {
    public FareDetailModel toModel(FareDto data) {
        if (data == null) return null;
        return FareDetailModel
                .builder()
                .id(data.getId())
                .distanceFare(data.getDistanceFare())
                .baseFare(data.getBaseFare())
                .paymentStatus(data.getPaymentStatus())
                .currency(data.getCurrency())
                .discount(data.getDiscount())
                .bookingFee(data.getBookingFee())
                .surgeMultiplier(data.getSurgeMultiplier())
                .tax(data.getTax())
                .timeFare(data.getTimeFare())
                .totalFare(data.getTotalFare())
                .createdAt(data.getCreatedAt())
                .updatedAt(data.getUpdatedAt())
                .build();
    }


    public FareDto toDto(FareDetailModel data) {
        if (data == null) return null;
        return FareDto
                .builder()
                .id(data.getId())
                .distanceFare(data.getDistanceFare())
                .baseFare(data.getBaseFare())
                .paymentStatus(data.getPaymentStatus())
                .currency(data.getCurrency())
                .discount(data.getDiscount())
                .bookingFee(data.getBookingFee())
                .surgeMultiplier(data.getSurgeMultiplier())
                .tax(data.getTax())
                .timeFare(data.getTimeFare())
                .totalFare(data.getTotalFare())
                .createdAt(data.getCreatedAt())
                .updatedAt(data.getUpdatedAt())
                .build();
    }
}
