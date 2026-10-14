package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity(name = "DriverSuspendModel")
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "driver_suspensd", indexes = {
        @Index(name = "driver_suspend_idx", columnList = "driver_id"),
        @Index(name = "driver_suspended_by_idx", columnList = "suspended_by_id"),
        @Index(name = "driver_unsuspended_by_idx", columnList = "unsuspended_by_id"),
        @Index(name = "driver_is_suspended_idx", columnList = "is_suspended")
})
public class DriverSuspendEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private DriverEntity driver;

    private boolean isSuspended;
    private String reason;
    private LocalDateTime expiry;

    @Column(name = "suspended_by_id")
    private Long suspendedById;

    @Column(name = "unsuspended_by_id")
    private Long unsuspendedById;

    private LocalDateTime suspendedAt;
    private LocalDateTime unsuspendedAt;

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
