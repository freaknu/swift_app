package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Data
@Entity(name = "RideDriverModel")
@Builder
@Table(name = "ride_driver", indexes = {
        @Index(name = "ride_driver_idx", columnList = "driver_id"),
        @Index(name = "rider_user_idx", columnList = "auth_user_id"),
        @Index(name = "ride_driver_ride_id_idx", columnList = "ride_id")
})
@AllArgsConstructor
@NoArgsConstructor
public class RideDriverEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name = "driver_id", nullable = false)
    private Long driverId;
    @Column(name = "auth_user_id", nullable = false)
    private Long authUserId;
    @Column(nullable = false)
    private String driverName;
    private String driverEmail;
    private String driverPhone;
    private String driverGender;
    private String driverProfileImage;
    private Boolean isActive;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    private RideEntity ride;

    @CreatedDate
    private LocalDateTime rideDriverCreatedAt;
    @UpdateTimestamp
    private LocalDateTime rideDriverUpdatedAt;

    @PrePersist
    protected void onCreate() {
        this.rideDriverCreatedAt = LocalDateTime.now();
        this.rideDriverUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.rideDriverUpdatedAt = LocalDateTime.now();
    }
}
