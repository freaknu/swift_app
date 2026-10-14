package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "ride_completion_outbox", uniqueConstraints =
        @UniqueConstraint(name = "ride_completion_topic_key", columnNames = {"topic", "event_key"}))
@Getter
@Setter
@NoArgsConstructor
public class RideCompletionOutboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String topic;
    @Column(name = "event_key", nullable = false)
    private String eventKey;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    // Delivery metadata is durable; an application restart does not reset retry budgets.
    @Column(name = "delivery_attempts", nullable = false, columnDefinition = "integer default 0")
    private int deliveryAttempts;
    private Instant nextAttemptAt;
    private Instant leaseUntil;
    @Column(length = 36) private String deliveryToken;
    private Instant quarantinedAt;
    @Column(length = 160) private String lastFailureType;
}
