package com.swiftapp_backend.userservice.user.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "user_preferences",
        indexes = @Index(name = "idx_pref_user_id", columnList = "user_id")
)
public class UserPreferencesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(length = 20)
    private String language;

    @Column(length = 20)
    private String theme;

    private boolean pushNotificationEnabled;

    private boolean emailNotificationEnabled;

    private boolean smsNotificationEnabled;

    @Column(length = 50)
    private String preferredPaymentMethod;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.language == null) this.language = "en";
        if (this.theme == null) this.theme = "SYSTEM";
        this.updatedAt = LocalDateTime.now();
    }
}
