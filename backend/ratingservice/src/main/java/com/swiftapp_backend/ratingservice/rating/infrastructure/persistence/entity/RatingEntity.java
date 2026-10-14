package com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.entity;

import com.swiftapp_backend.ratingservice.rating.domain.enums.UserType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
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
import java.util.ArrayList;
import java.util.List;

@Data
@Entity(name = "RatingModel")
@Builder
@Table(
        name = "rating",
        indexes = {
                @Index(name = "rating_rider_idx", columnList = "rider_id"),
                @Index(name = "rating_driver_idx", columnList = "driver_id")
        }
)
@AllArgsConstructor
@NoArgsConstructor
public class RatingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rider_id")
    private Long riderId;

    @Column(name = "driver_id")
    private Long driverId;

    private double averageRating;

    @Enumerated(EnumType.STRING)
    private UserType userType;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(
            mappedBy = "rating",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<RateUserEntity> allRatings;

    @CreatedDate
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.averageRating = 0.0;
        this.allRatings = new ArrayList<>();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
