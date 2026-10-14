package com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.entity;
import com.swiftapp_backend.paymentservice.payment.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name = "ride_payments")
@Getter @Setter @NoArgsConstructor
public class PaymentEntity {
    @Id private Long rideId;
    @Column(nullable = false) private Long riderId;
    @Column(nullable = false) private Long driverId;
    @Column(nullable = false) private long amount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, unique = true, length = 40) private String receipt;
    @Column(unique = true) private String razorpayOrderId;
    @Column(unique = true) private String razorpayPaymentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentStatus status;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    // Committed before a provider POST. Ambiguous responses must reconcile this receipt, not POST again.
    private Instant orderCreationAttemptedAt;
    @Column(length = 36) private String providerOperationToken;
    private Instant providerOperationExpiresAt;
    // Intentionally nullable, with no database default: rows from older versions have an unknown POST outcome.
    private Integer providerWorkflowVersion;
}
