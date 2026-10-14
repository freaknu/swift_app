package com.swiftapp_backend.riderservice.ride.infrastructure.messaging.kafka;

import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaRideCompletionOutboxRepository;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideCompletionOutboxEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:completionoutbox;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "ride.completion-outbox.enabled=false"})
@ActiveProfiles("test")
class RideCompletionOutboxTest {
    @TestConfiguration
    static class Config {
        @Bean @Primary
        RecordingKafka recordingKafka() { return new RecordingKafka(); }
    }

    static class RecordingKafka extends KafkaTemplate<String, Object> {
        boolean fail;
        Set<String> failedKeys = new HashSet<>(), pendingKeys = new HashSet<>(), failedTopics = new HashSet<>();
        List<String> keys = new ArrayList<>();
        List<CompletableFuture<SendResult<String, Object>>> pending = new ArrayList<>();
        int sent;
        Object lastPayload;
        RecordingKafka() {
            super(new DefaultKafkaProducerFactory<>(Map.of("bootstrap.servers", "127.0.0.1:1")));
        }
        @Override
        public CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object value) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            sent++; keys.add(key);
            lastPayload = value;
            if (fail || failedKeys.contains(key) || failedTopics.contains(topic))
                return CompletableFuture.failedFuture(new IllegalStateException("broker unavailable; private data omitted"));
            if (pendingKeys.contains(key)) {
                var future = new CompletableFuture<SendResult<String, Object>>(); pending.add(future); return future;
            }
            return CompletableFuture.completedFuture(new SendResult<>(new ProducerRecord<>(topic, key, value),
                    new RecordMetadata(new TopicPartition(topic, 0), 0, 0, 0, 0, 0)));
        }
    }

    @Autowired RideCompletionOutbox outbox;
    @Autowired ObjectMapper mapper;
    MutableClock clock;
    RideCompletionOutbox worker;
    RideCompletionOutbox newWorker() { return new RideCompletionOutbox(repository, mapper, kafka, manager, clock); }
    RideCompletionOutboxEntity row(String topic, String key) {
        var entry = new RideCompletionOutboxEntity(); entry.setTopic(topic); entry.setEventKey(key);
        entry.setPayload("{\"rideId\":" + key + "}"); return repository.save(entry);
    }
    RideCompletionOutboxEntity read(long id) { return repository.findById(id).orElseThrow(); }
    void acknowledge(int index) {
        kafka.pending.get(index).complete(new SendResult<>(new ProducerRecord<>("ride.completed", "1", Map.of()),
            new RecordMetadata(new TopicPartition("ride.completed", 0), 0, 0, 0, 0, 0)));
    }
    @Autowired KafkaProducerService producer;
    @Autowired JpaRideCompletionOutboxRepository repository;
    @Autowired PlatformTransactionManager manager;
    @Autowired RecordingKafka kafka;

    @BeforeEach
    void clean() {
        repository.deleteAll(); kafka.sent = 0; kafka.fail = false;
        kafka.failedKeys.clear(); kafka.pendingKeys.clear(); kafka.failedTopics.clear(); kafka.keys.clear(); kafka.pending.clear();
        clock = new MutableClock(); worker = newWorker();
    }

    @Test
    void rollbackCannotEmitACompletionAndCommittedEventsRetryUntilBrokerAcknowledges() {
        TransactionTemplate transaction = new TransactionTemplate(manager);
        Map<String, Object> event = Map.of("rideId", 15, "riderId", 16, "driverId", 17,
                "totalFare", new java.math.BigDecimal("900000000000000.01"), "currency", "INR");
        transaction.executeWithoutResult(status -> {
            producer.publish("ride.completed", "15", event);
            assertThat(repository.count()).isEqualTo(1);
            assertThat(kafka.sent).isZero();
            status.setRollbackOnly();
        });
        assertThat(repository.count()).isZero();
        transaction.executeWithoutResult(status -> producer.publish("ride.completed", "15", event));
        kafka.fail = true;
        worker.flush();
        assertThat(repository.count()).isEqualTo(1);
        kafka.fail = false;
        clock.advance(1);
        worker.flush();
        assertThat(repository.count()).isZero();
        assertThat(kafka.lastPayload).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) kafka.lastPayload).get("rideId")).isEqualTo(15);
        assertThat(((Map<?, ?>) kafka.lastPayload).get("totalFare"))
                .isEqualTo(new java.math.BigDecimal("900000000000000.01"));
        assertThat(kafka.sent).isEqualTo(2);
    }

    @Test void failedRideDoesNotDelayAnotherRideAndBackoffSurvivesRestart() {
        var a = row("ride.completed", "1"); var b = row("ride.completed", "2");
        kafka.failedKeys.add("1"); worker.flush();
        assertThat(repository.existsById(b.getId())).isFalse();
        assertThat(read(a.getId()).getDeliveryAttempts()).isEqualTo(1);
        assertThat(read(a.getId()).getNextAttemptAt()).isEqualTo(clock.instant().plusSeconds(1));
        newWorker().flush(); assertThat(kafka.sent).isEqualTo(2);
        clock.advance(1); newWorker().flush();
        assertThat(read(a.getId()).getNextAttemptAt()).isEqualTo(clock.instant().plusSeconds(2));
        clock.advance(1); newWorker().flush(); assertThat(kafka.sent).isEqualTo(3);
        clock.advance(1); kafka.failedKeys.clear(); newWorker().flush();
        assertThat(repository.count()).isZero();
    }

    @Test void legacyTopicFailureDoesNotBlockCanonicalPaymentTriggerForSameRide() {
        var legacy = row("ride-completed", "1"); var canonical = row("ride.completed", "1");
        kafka.failedTopics.add("ride-completed"); worker.flush();
        assertThat(read(legacy.getId()).getDeliveryAttempts()).isEqualTo(1);
        assertThat(repository.existsById(canonical.getId())).isFalse();
        assertThat(kafka.sent).isEqualTo(2);
    }

    @Test void exhaustedDeliveryIsQuarantinedAndOperatorCanReplayAfterCorrection() {
        var entry = row("ride.completed", "1"); row("ride.completed", "2"); kafka.failedKeys.add("1");
        for (int attempt = 0; attempt < RideCompletionOutbox.MAX_ATTEMPTS; attempt++) {
            newWorker().flush(); clock.advance(60);
        }
        var stored = read(entry.getId());
        assertThat(stored.getQuarantinedAt()).isNotNull();
        assertThat(stored.getDeliveryAttempts()).isEqualTo(8);
        assertThat(stored.getLastFailureType()).isEqualTo("IllegalStateException");
        newWorker().flush(); assertThat(kafka.keys.stream().filter("1"::equals)).hasSize(8);
        assertThat(repository.findByQuarantinedAtIsNotNullOrderByQuarantinedAtAsc(org.springframework.data.domain.PageRequest.of(0, 20)))
            .extracting(RideCompletionOutboxEntity::getId).containsExactly(entry.getId());
        kafka.failedKeys.clear(); assertThat(worker.replayQuarantined(entry.getId())).isTrue();
        assertThat(worker.replayQuarantined(entry.getId())).isFalse();
        assertThat(read(entry.getId()).getDeliveryAttempts()).isZero();
        newWorker().flush(); assertThat(repository.count()).isZero();
    }

    @Test void invalidPersistedPayloadIsQuarantinedBeforeSendAndOtherRideContinues() {
        var invalid = row("ride.completed", "1"); invalid.setPayload("not-json"); repository.save(invalid);
        var valid = row("ride.completed", "2"); worker.flush();
        assertThat(read(invalid.getId()).getQuarantinedAt()).isNotNull();
        assertThat(read(invalid.getId()).getLastFailureType()).isEqualTo("JsonParseException");
        assertThat(repository.existsById(valid.getId())).isFalse();
        assertThat(kafka.keys).containsExactly("2");
        var corrected = read(invalid.getId()); corrected.setPayload("{\"rideId\":1}"); repository.save(corrected);
        assertThat(worker.replayQuarantined(invalid.getId())).isTrue(); worker.flush();
        assertThat(repository.count()).isZero();
    }

    @Test void asynchronousAcknowledgementDoesNotBlockAnotherRideOrAllowConcurrentClaim() {
        var pending = row("ride.completed", "1"); var ready = row("ride.completed", "2");
        kafka.pendingKeys.add("1"); worker.flush();
        assertThat(read(pending.getId()).getDeliveryToken()).isNotNull();
        assertThat(repository.existsById(ready.getId())).isFalse();
        newWorker().flush(); assertThat(kafka.sent).isEqualTo(2);
        acknowledge(0); assertThat(repository.count()).isZero();
    }

    @Test void expiredLeaseRecoversAndStaleAcknowledgementCannotDeleteNewClaim() {
        var entry = row("ride.completed", "1"); kafka.pendingKeys.add("1"); worker.flush();
        String token = read(entry.getId()).getDeliveryToken();
        clock.advance(31); newWorker().flush();
        assertThat(read(entry.getId()).getDeliveryToken()).isNotEqualTo(token);
        assertThat(read(entry.getId()).getDeliveryAttempts()).isEqualTo(2);
        acknowledge(0); assertThat(repository.existsById(entry.getId())).isTrue();
        acknowledge(1); assertThat(repository.count()).isZero();
    }

    @Test void finalAttemptCrashDoesNotExceedDeliveryBudget() {
        var entry = row("ride.completed", "1"); entry.setDeliveryAttempts(8);
        entry.setDeliveryToken("expired"); entry.setLeaseUntil(clock.instant().minusSeconds(1)); repository.save(entry);
        worker.flush(); assertThat(kafka.sent).isZero();
        assertThat(read(entry.getId()).getQuarantinedAt()).isNotNull();
        assertThat(read(entry.getId()).getLastFailureType()).isEqualTo("DeliveryLeaseExpired");
    }

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-14T10:00:00Z");
        void advance(long seconds) { now = now.plusSeconds(seconds); }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
