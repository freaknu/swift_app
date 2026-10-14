package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity(name = "DriverStatisticsModel")
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "driver_statistics", indexes = {
        @Index(name = "driver_statics_idx", columnList = "driver_id")
})
public class DriverStatisticsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Integer completedTrips;
    private Integer cancelledTrips;
    private double averageRating;
    private double acceptanceRate;
    private double completionRate;
    private double lifetimeDistance;
    private double lifetimeEarning;

    @OneToOne
    @JoinColumn(name = "driver_id")
    private DriverEntity driver;

    @CreatedDate
    private LocalDateTime createdAt;

    @UpdateTimestamp
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
