package com.swiftapp_backend.paymentservice.payment.domain.model;
import com.swiftapp_backend.paymentservice.payment.domain.enums.PaymentStatus;
import lombok.*;
import java.time.Instant;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Payment {
    private Long rideId;
    private Long riderId;
    private Long driverId;
    private long amount;
    private String currency;
    private String receipt;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private PaymentStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant orderCreationAttemptedAt;
    private String providerOperationToken;
    private Instant providerOperationExpiresAt;
    private Integer providerWorkflowVersion;
}
