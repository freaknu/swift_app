package com.swiftapp_backend.riderservice.ride.api.dto.offer;

import com.swiftapp_backend.riderservice.ride.domain.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class OfferResponseDto {
    private Long id;
    private String offerCode;
    private String title;
    private String description;
    private String rideTypeCode;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maximumDiscount;
    private BigDecimal minimumFare;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private boolean active = true;
    private Integer usageLimit;
    private Integer perUserLimit;
    private Integer usedCount = 0;
    private boolean newUserOnly = false;
    private boolean stackable = false;
}
