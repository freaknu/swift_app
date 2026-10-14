package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity;

import com.swiftapp_backend.driverservice.driver.domain.enums.DriverAvailability;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverPreferences;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverStatus;
import com.swiftapp_backend.driverservice.driver.domain.enums.Gender;
import com.swiftapp_backend.driverservice.driver.domain.enums.Language;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity(name = "DriverModel")
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "driver", indexes = {
        @Index(name = "driver_authuser_idx", columnList = "auth_user_id"),
        @Index(name = "driver_authuseremail_idx", columnList = "email")
})
public class DriverEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "auth_user_id")
    private Long authUserId;

    private String driverName;

    @Column(name = "email")
    private String email;

    @Column(unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String profileImage;

    private boolean isActive;

    @Enumerated(EnumType.STRING)
    private DriverStatus driverStatus;

    private boolean isAcceptingRides;

    @Enumerated(EnumType.STRING)
    private DriverAvailability driverAvailability;

    private boolean isVerified;

    @ElementCollection(fetch = FetchType.LAZY)
    @JoinTable(name = "preferences")
    @Enumerated(EnumType.STRING)
    private List<DriverPreferences> driverPreferences;

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY,
            orphanRemoval = true, cascade = CascadeType.ALL)
    private List<VehicleEntity> allVehicles;

    private LocalDate dateOfBirth;

    @ElementCollection(fetch = FetchType.LAZY)
    @JoinTable(name = "languages")
    @Enumerated(EnumType.STRING)
    private List<Language> languages;

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DriverDocumentEntity> allDocuments;

    @CreatedDate
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isAcceptingRides = false;
        this.isActive = true;
        this.isVerified = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
