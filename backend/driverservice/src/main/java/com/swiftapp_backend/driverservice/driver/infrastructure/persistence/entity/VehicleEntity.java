package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity;

import com.swiftapp_backend.driverservice.driver.domain.enums.VehicleType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
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
import java.util.List;

@Getter
@Setter
@Entity(name = "VehicleModel")
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vehicle", indexes = {
        @Index(name = "vehicle_driver_idx", columnList = "driver_id"),
        @Index(name = "auth_user_idx", columnList = "auth_user_id")
})
public class VehicleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private DriverEntity driver;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    private Long authUserId;
    private String brand;
    private String model;
    private int year;
    private String registrationNumber;
    private String fuelType;
    private String color;
    private int seatCapacity;
    private boolean isActive;
    private boolean isEv;
    private int manufactureYear;
    private int seats;
    private Boolean isVerified;

    @ElementCollection
    @JoinTable(name = "vehicle_images")
    private List<String> vehicleImage;

    @CreatedDate
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.isVerified = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
