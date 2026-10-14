package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity;

import com.swiftapp_backend.riderservice.ride.domain.enums.RideType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity(name = "RideTypesModel")
@Table(
        name = "ride_types",
        indexes = {
                @Index(name = "ride_type_code_idx", columnList = "code"),
                @Index(name = "ride_type_active_idx", columnList = "is_active"),
                @Index(name = "ride_type_sort_idx", columnList = "sort_order")
        }
)
@NoArgsConstructor
@AllArgsConstructor
public class RideTypesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(length = 255)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "ride_type", nullable = false, length = 50)
    private RideType rideType;
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
    @Column(name = "max_passengers", nullable = false)
    @Builder.Default
    private Integer maxPassengers = 1;
    @Column(name = "scheduled_supported", nullable = false)
    @Builder.Default
    private boolean scheduledSupported = true;
    @Column(name = "cash_supported", nullable = false)
    @Builder.Default
    private boolean cashSupported = true;
    @Column(name = "online_payment_supported", nullable = false)
    @Builder.Default
    private boolean onlinePaymentSupported = true;
}
