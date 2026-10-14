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
@Entity(name = "DriverMedicalInformationModel")
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "driver_medical_information", indexes = {
        @Index(name = "driver_info_idx", columnList = "driver_id"),
        @Index(name = "driver_info_user_idx", columnList = "auth_user_id")
})
public class DriverMedicalInformationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long authUserId;
    private Character bloodGroup;
    private String medicalCondition;
    private boolean sosEnabled;

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
