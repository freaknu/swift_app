package com.swiftapp_backend.riderservice.ride.infrastructure.messaging.kafka;

import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideCompletionOutboxEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaRideCompletionOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Claims commit before network I/O; broker acknowledgement and retry state each
 * use a separate short transaction. The durable lease recovers process crashes.
 * Kafka delivery remains at least once, including an acknowledgement/commit gap.
 */
@Component
@Slf4j
public class RideCompletionOutbox {
    public static final int MAX_ATTEMPTS = 8;
    public static final Duration LEASE_DURATION = Duration.ofSeconds(30);
    private static final int MAX_IN_FLIGHT = 16;
    private final JpaRideCompletionOutboxRepository repository;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final AtomicInteger inFlight = new AtomicInteger();

    @Autowired
    public RideCompletionOutbox(JpaRideCompletionOutboxRepository repository, ObjectMapper mapper, KafkaTemplate<String, Object> kafka,
            PlatformTransactionManager manager) {
        this(repository, mapper, kafka, manager, Clock.systemUTC());
    }

    public RideCompletionOutbox(JpaRideCompletionOutboxRepository repository, ObjectMapper mapper, KafkaTemplate<String, Object> kafka,
            PlatformTransactionManager manager, Clock clock) {
        this.repository = repository;
        this.kafka = kafka;
        this.mapper = mapper;
        this.clock = clock;
        this.transactions = new TransactionTemplate(manager);
        this.transactions.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String topic, String key, Object event) {
        RideCompletionOutboxEntity entry = new RideCompletionOutboxEntity();
        entry.setTopic(topic);
        entry.setEventKey(key);
        try {
            entry.setPayload(mapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize ride completion", ex);
        }
        repository.save(entry);
    }

    @Scheduled(fixedDelayString = "${ride.completion-outbox.delay-ms:1000}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void flush() {
        for (var id : repository.findDeliverableIds(clock.instant(), PageRequest.of(0, 50))) {
            if (!reserveSlot()) return;
            RideCompletionOutboxEntity row;
            try {
                row = transactions.execute(status -> claim(id));
            } catch (RuntimeException ex) {
                inFlight.decrementAndGet();
                // A database failure is independent of other aggregate heads.
                log.warn("Could not claim outbox event {}: {}", id, ex.getClass().getSimpleName());
                continue;
            }
            if (row == null) {
                inFlight.decrementAndGet();
                continue;
            }
            try {
                kafka.send(row.getTopic(), row.getEventKey(), mapper.readerFor(Map.class).with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readValue(row.getPayload()))
                    .orTimeout(10, TimeUnit.SECONDS)
                    .whenComplete((result, failure) -> finish(row, failure));
            } catch (Exception ex) {
                finish(row, ex);
            }
        }
    }

    private boolean reserveSlot() {
        int current;
        do {
            current = inFlight.get();
            if (current >= MAX_IN_FLIGHT) return false;
        } while (!inFlight.compareAndSet(current, current + 1));
        return true;
    }

    private RideCompletionOutboxEntity claim(Long id) {
        var row = repository.lockById(id).orElse(null);
        var now = clock.instant();
        if (row == null || row.getQuarantinedAt() != null
                || (row.getNextAttemptAt() != null && row.getNextAttemptAt().isAfter(now))
                || (row.getLeaseUntil() != null && row.getLeaseUntil().isAfter(now))
                || repository.countByTopicAndEventKeyAndIdLessThan(row.getTopic(), row.getEventKey(), row.getId()) > 0) return null;
        if (row.getDeliveryAttempts() >= MAX_ATTEMPTS) {
            quarantine(row, "DeliveryLeaseExpired");
            return null;
        }
        row.setDeliveryAttempts(row.getDeliveryAttempts() + 1);
        row.setDeliveryToken(UUID.randomUUID().toString());
        row.setLeaseUntil(now.plus(LEASE_DURATION));
        repository.save(row);
        return row;
    }

    private void finish(RideCompletionOutboxEntity claimed, Throwable failure) {
        try {
            transactions.executeWithoutResult(status -> {
                var row = repository.lockById(claimed.getId()).orElse(null);
                // An expired worker cannot overwrite a later claim or replay.
                if (row == null || !claimed.getDeliveryToken().equals(row.getDeliveryToken())) return;
                if (failure == null) {
                    repository.delete(row);
                    return;
                }
                Throwable cause = rootCause(failure);
                if (permanent(cause) || row.getDeliveryAttempts() >= MAX_ATTEMPTS) {
                    quarantine(row, cause.getClass().getSimpleName());
                } else {
                    row.setNextAttemptAt(clock.instant().plusSeconds(retrySeconds(row.getDeliveryAttempts())));
                    row.setLastFailureType(cause.getClass().getSimpleName());
                    row.setDeliveryToken(null);
                    row.setLeaseUntil(null);
                    repository.save(row);
                    log.warn("Outbox event {} will retry after attempt {}", row.getId(), row.getDeliveryAttempts());
                }
            });
        } catch (RuntimeException ex) {
            // Leave the committed lease intact. It becomes claimable after expiry.
            log.warn("Could not record outbox result for {}: {}", claimed.getId(), ex.getClass().getSimpleName());
        } finally {
            inFlight.decrementAndGet();
        }
    }

    private void quarantine(RideCompletionOutboxEntity row, String failureType) {
        row.setQuarantinedAt(clock.instant());
        row.setLastFailureType(failureType);
        row.setDeliveryToken(null);
        row.setLeaseUntil(null);
        row.setNextAttemptAt(null);
        repository.save(row);
        log.error("Outbox event {} quarantined after {} attempts; operator replay required",
                row.getId(), row.getDeliveryAttempts());
    }

    /** Operator-only replay after correcting the cause; intentionally no public HTTP endpoint. */
    public boolean replayQuarantined(Long id) {
        return Boolean.TRUE.equals(transactions.execute(status -> {
            var row = repository.lockById(id).orElse(null);
            if (row == null || row.getQuarantinedAt() == null) return false;
            row.setQuarantinedAt(null);
            row.setDeliveryAttempts(0);
            row.setNextAttemptAt(clock.instant());
            row.setDeliveryToken(null);
            row.setLeaseUntil(null);
            row.setLastFailureType(null);
            repository.save(row);
            return true;
        }));
    }

    private static long retrySeconds(int attempt) {
        return Math.min(60, 1L << Math.min(attempt - 1, 6));
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cause = failure;
        for (int i = 0; i < 20 && cause.getCause() != null && cause.getCause() != cause; i++) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static boolean permanent(Throwable cause) {
        return cause instanceof SerializationException || cause instanceof InvalidTopicException
                || cause instanceof RecordTooLargeException || cause instanceof AuthorizationException
                || cause instanceof JsonProcessingException;
    }
}
