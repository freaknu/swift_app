package com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "payment_orders", indexes = @Index(name = "idx_payment_orders_ride", columnList = "rideId"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PaymentOrderEntity {
    @Id private String orderId;
    @Column(nullable = false) private Long rideId;
    @Column(nullable = false, unique = true, length = 40) private String receipt;
}
