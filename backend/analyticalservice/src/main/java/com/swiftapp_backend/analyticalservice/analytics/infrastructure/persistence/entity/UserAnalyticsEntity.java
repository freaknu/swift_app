package com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.entity;

import com.swiftapp_backend.analyticalservice.analytics.domain.enums.AppUserRoles;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "user_analytics",
        indexes = {
                @Index(name = "user_analytics_user_idx", columnList = "auth_user_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnalyticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "auth_user_id", nullable = false, unique = true)
    private Long authUserId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppUserRoles role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
