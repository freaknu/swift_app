package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity;

import com.swiftapp_backend.driverservice.driver.domain.enums.DriverRideStatisticsEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "driver_ride_statistics_event", uniqueConstraints =
        @UniqueConstraint(name = "driver_ride_statistics_ride_uq", columnNames = "ride_id"))
public class DriverRideStatisticsEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ride_id", nullable = false)
    private Long rideId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private DriverRideStatisticsEventType eventType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @PrePersist
    void onCreate() {
        if (processedAt == null) processedAt = LocalDateTime.now();
    }
}
