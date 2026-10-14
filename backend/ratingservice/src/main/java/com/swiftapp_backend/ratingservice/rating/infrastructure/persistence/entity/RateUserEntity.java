package com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "RateUserModel")
@Builder
@Table(
        name = "rate_user",
        indexes = {
                @Index(name = "rate_user_idx", columnList = "user_id"),
                @Index(name = "rate_user_email_idx", columnList = "email"),
                @Index(name = "rate_user_rating_idx", columnList = "rating_id")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_rate_user_ride_auth_user",
                columnNames = {"ride_id", "rater_auth_user_id"})
)
public class RateUserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "rater_auth_user_id")
    private Long raterAuthUserId;

    @Column(name = "ride_id")
    private Long rideId;

    private String email;
    private String name;
    private String imageUrl;
    private double rate;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne
    @JoinColumn(name = "rating_id")
    private RatingEntity rating;

    @CreatedDate
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
