package com.swiftapp_backend.authservice.authentication.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "UserCredentials")
@Table(
        name = "user_credentials",
        indexes = @Index(name = "user_credential_idx", columnList = "user_id")
)
public class UserCredentialsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;
    private String deviceName;
    private String os;
    private String deviceModel;

    @Column(columnDefinition = "TEXT")
    private String fingerPrint;

    private String ipAddress;
    private int appLockPin;

    @Column(name = "is_app_locked")
    private boolean isAppLocked;

    @Column(name = "is_logged_out")
    private boolean isLoggedOut;

    @CreatedDate
    private LocalDateTime loggedInAt;

    @PrePersist
    protected void onCreate() {
        this.loggedInAt = LocalDateTime.now();
    }
}
