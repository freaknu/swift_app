package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity;

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
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity(name = "RideUserModel")
@Table(name = "ride_user", indexes = {
        @Index(name = "ride_user_auth_idx", columnList = "auth_user_id"),
        @Index(name = "ride_user_ride_idx", columnList = "ride_id")
})
@AllArgsConstructor
@NoArgsConstructor
public class RideUserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long authUserId;
    private String name;
    private String phoneNumber;
    private Boolean isActive;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    private RideEntity ride;
    private LocalDateTime rideCreatedAt;
    private LocalDateTime rideUpdatedAt;

    @PrePersist
    protected void onCreate() {
        this.rideCreatedAt = LocalDateTime.now();
        this.rideUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.rideUpdatedAt = LocalDateTime.now();
    }
}
