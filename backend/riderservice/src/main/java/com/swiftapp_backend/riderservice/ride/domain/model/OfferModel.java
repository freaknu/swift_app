package com.swiftapp_backend.riderservice.ride.domain.model;

import com.swiftapp_backend.riderservice.ride.domain.enums.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class OfferModel {

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

    @Builder.Default
    private boolean active = true;

    private Integer usageLimit;

    private Integer perUserLimit;

    @Builder.Default
    private Integer usedCount = 0;

    @Builder.Default
    private boolean newUserOnly = false;

    @Builder.Default
    private boolean stackable = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
