package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository;

import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideCompletionOutboxEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JpaRideCompletionOutboxRepository extends JpaRepository<RideCompletionOutboxEntity, Long> {
    // Legacy and canonical completion topics are independent streams. A legacy
    // topic failure must not prevent the canonical payment trigger from arriving.
    @Query("""
        select e.id from RideCompletionOutboxEntity e
        where e.quarantinedAt is null
          and (e.nextAttemptAt is null or e.nextAttemptAt <= :now)
          and (e.leaseUntil is null or e.leaseUntil <= :now)
          and not exists (select p.id from RideCompletionOutboxEntity p
              where p.topic = e.topic and p.eventKey = e.eventKey and p.id < e.id)
        order by e.id
        """)
    List<Long> findDeliverableIds(Instant now, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from RideCompletionOutboxEntity e where e.id = :id")
    Optional<RideCompletionOutboxEntity> lockById(Long id);

    long countByTopicAndEventKeyAndIdLessThan(String topic, String eventKey, Long id);

    List<RideCompletionOutboxEntity> findByQuarantinedAtIsNotNullOrderByQuarantinedAtAsc(Pageable page);
}
