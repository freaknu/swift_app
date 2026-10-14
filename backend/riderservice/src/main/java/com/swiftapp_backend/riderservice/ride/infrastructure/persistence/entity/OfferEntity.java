package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity;

import com.swiftapp_backend.riderservice.ride.domain.enums.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "OfferModel")
@Table(
        name = "offers",
        indexes = {
                @Index(name = "offer_code_idx", columnList = "offer_code"),
                @Index(name = "offer_ride_type_idx", columnList = "ride_type_code"),
                @Index(name = "offer_active_idx", columnList = "is_active"),
                @Index(name = "offer_validity_idx", columnList = "valid_from,valid_until")
        }
)
public class OfferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_code", nullable = false, unique = true, length = 50)
    private String offerCode;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String description;

    @Column(name = "ride_type_code", length = 50)
    private String rideTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal maximumDiscount;

    private BigDecimal minimumFare;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "new_user_only", nullable = false)
    @Builder.Default
    private boolean newUserOnly = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean stackable = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.usedCount == null) {
            this.usedCount = 0;
        }

        if (!this.active) {
            this.active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
