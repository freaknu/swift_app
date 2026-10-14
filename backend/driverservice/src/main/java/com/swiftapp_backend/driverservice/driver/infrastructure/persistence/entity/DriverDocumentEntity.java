package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity;

import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import java.util.Set;

@Getter
@Setter
@Entity(name = "DriverDocumentModel")
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "driver_document", indexes = {
        @Index(name = "driver_document_idx", columnList = "driver_id"),
        @Index(name = "driver_document_user_idx", columnList = "auth_user_id")
})
public class DriverDocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private DriverEntity driver;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private Long authUserId;
    private boolean isActive;
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;
    private Long verifiedBy;
    private String remarks;

    @ElementCollection(fetch = FetchType.LAZY)
    @JoinTable(name = "document_status")
    private Set<VerificationStatus> allStatus;

    @CreatedDate
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.verificationStatus = VerificationStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
