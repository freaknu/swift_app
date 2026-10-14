package com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.repository;

import com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.entity.PaymentOutboxEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentOutboxJpaRepository extends JpaRepository<PaymentOutboxEntity, String> {
    // Only a ride's oldest unpublished event is eligible. A poison ride cannot
    // occupy the batch with all of its later events or overtake its own retry.
    @Query("""
        select e.eventId from PaymentOutboxEntity e
        where e.publishedAt is null and e.quarantinedAt is null
          and (e.nextAttemptAt is null or e.nextAttemptAt <= :now)
          and (e.leaseUntil is null or e.leaseUntil <= :now)
          and not exists (select p.eventId from PaymentOutboxEntity p
              where p.rideId = e.rideId and p.publishedAt is null
              and p.deliverySequence < e.deliverySequence)
        order by e.deliverySequence
        """)
    List<String> findDeliverableIds(Instant now, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from PaymentOutboxEntity e where e.eventId = :eventId")
    Optional<PaymentOutboxEntity> lockByEventId(String eventId);

    @Query("""
        select count(p) from PaymentOutboxEntity p where p.rideId = :rideId and p.publishedAt is null
          and p.deliverySequence < :deliverySequence
        """)
    long countPredecessors(Long rideId, Long deliverySequence);

    List<PaymentOutboxEntity> findByQuarantinedAtIsNotNullOrderByQuarantinedAtAsc(Pageable page);
}
