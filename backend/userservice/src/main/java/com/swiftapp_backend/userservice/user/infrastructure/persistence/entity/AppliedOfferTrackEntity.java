package com.swiftapp_backend.userservice.user.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Data
@Entity(name = "AppliedOfferTrackModel")
@Builder
@Table(
        name = "applied_offer",
        indexes = {
                @Index(name = "offer_user_idx", columnList = "auth_user_id"),
                @Index(name = "offer_rider_idx", columnList = "rider_id"),
                @Index(name = "offer_idx", columnList = "offer_id")
        }
)
@AllArgsConstructor
@NoArgsConstructor
public class AppliedOfferTrackEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "offer_id")
    private Long offerId;

    @Column(name = "auth_user_id")
    private Long authUserId;

    @Column(name = "rider_id")
    private Long riderId;

    @CreatedDate
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
