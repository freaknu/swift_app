package com.swiftapp_backend.chatservice.chat.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Entity
@Table(name = "chat_session",indexes = {
        @Index(name = "chat_driver_idx",columnList = "driver_id"),
        @Index(name = "chat_rider_idx",columnList = "rider_id"),
        @Index(name = "chat_ride_idx",columnList = "ride_id")
})
@RequiredArgsConstructor
@AllArgsConstructor
public class ChatSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String sessionNameForDriver;
    private String sessionNameForRider;
    @Column(name = "driver_id")
    private Long driverId;
    @Column(name = "rider_id")
    private Long riderId;
    @Column(name = "ride_id", unique = true)
    private Long rideId;
    @Column(name = "ride_status")
    private String rideStatus;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
