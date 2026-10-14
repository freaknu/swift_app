package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity;

import com.swiftapp_backend.riderservice.ride.domain.enums.Currency;
import com.swiftapp_backend.riderservice.ride.domain.enums.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "FareDetailModel")
@Table(
        name = "fare_details",
        indexes = {
                @Index(name = "fare_ride_idx", columnList = "ride_id")
        }
)
public class FareDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal baseFare;

    private BigDecimal distanceFare;

    private BigDecimal timeFare;

    private BigDecimal bookingFee;

    private BigDecimal tax;

    private BigDecimal surgeMultiplier;

    private BigDecimal discount;

    private BigDecimal totalFare;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private Currency currency;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private PaymentStatus paymentStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false, unique = true)
    private RideEntity ride;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
