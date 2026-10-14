package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity;

import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import com.swiftapp_backend.riderservice.ride.domain.enums.RideType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "RideModel")
@Table(
        name = "rides",
        indexes = {
                @Index(name = "ride_rider_idx", columnList = "rider_id"),
                @Index(name = "rides_driver_idx", columnList = "driver_id"),
                @Index(name = "ride_status_idx", columnList = "current_status"),
                @Index(name = "rides_type_code_idx", columnList = "ride_type_code")
        }
)
public class RideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private Long riderId;

    @Column(name = "active_rider_id", unique = true)
    private Long activeRiderId;

    private Long driverId;

    @Column(name = "active_driver_id", unique = true)
    private Long activeDriverId;

    private Long vehicleId;

    private String rideTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 30)
    @Builder.Default
    private RideStatus currentStatus = RideStatus.REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "ride_type")
    private RideType rideType;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double destinationLatitude;

    private Double destinationLongitude;

    private Double estimatedDistance;

    private Double actualDistance;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    private Long estimatedDuration;

    private Long actualDuration;

    private LocalDateTime requestedAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime driverArrivedAt;

    private LocalDateTime startedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRideCancelled = false;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private Long cancelledBy;

    private String cancellationReason;

    @OneToOne
    @JoinColumn(name = "offer_id")
    private OfferEntity offer;

    @OneToOne(
            mappedBy = "ride",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private FareDetailEntity fareDetail;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RideStatusHistoryEntity> rideHistory = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.isCompleted == null) this.isCompleted = false;
        if (this.isRideCancelled == null) this.isRideCancelled = false;
        if (this.currentStatus == null) this.currentStatus = RideStatus.REQUESTED;
        if (this.activeRiderId == null && this.riderId != null) this.activeRiderId = this.riderId;
    }
}
