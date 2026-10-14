package com.swiftapp_backend.locationservice.location.infrastructure.persistence.entity;

import com.swiftapp_backend.locationservice.location.domain.enums.Direction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import java.time.LocalDateTime;


@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "location",indexes = {
        @Index(name = "driver_location_driver_idx",columnList = "driver_id"),
        @Index(name = "driver_location_rider_idx",columnList = "rider_id"),
        @Index(name = "driver_location_latitude_idx",columnList = "latitude"),
        @Index(name = "driver_location_longitude_idx",columnList = "longitude"),
        @Index(name = "driver_location_ride_idx",columnList = "ride_id")
})
public class LocationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "ride_id")
    private Long rideId;

    @Column(name = "rider_id")
    private Long riderId;

    @Column(name = "latitude")
    private double latitude;

    @Column(name = "longitude")
    private double longitude;

    private String nearByPlace;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    @CreatedDate
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
