package com.swiftapp_backend.paymentservice.payment;

import com.swiftapp_backend.paymentservice.payment.infrastructure.kafka.service.PaymentOutboxDispatcher;
import com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.entity.PaymentOutboxEntity;
import com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.repository.PaymentOutboxJpaRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:paymentoutbox;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentOutboxDispatcherTest {
    @Autowired PaymentOutboxJpaRepository repository;
    @Autowired PlatformTransactionManager manager;
    MutableClock clock;
    RecordingKafka kafka;
    PaymentOutboxDispatcher dispatcher;

    @BeforeEach void setup() {
        repository.deleteAll();
        clock = new MutableClock();
        kafka = new RecordingKafka();
        dispatcher = worker();
    }
    PaymentOutboxDispatcher worker() { return new PaymentOutboxDispatcher(repository, kafka, manager, clock); }
    PaymentOutboxEntity row(String id, long ride, long second) {
        var row = new PaymentOutboxEntity();
        row.setEventId(id); row.setRideId(ride); row.setTopic("payment.completed");
        row.setPayload("{}"); row.setCreatedAt(Instant.EPOCH.plusSeconds(second));
        return repository.save(row);
    }
    PaymentOutboxEntity read(String id) { return repository.findById(id).orElseThrow(); }

    @Test void failingRideDoesNotStopOtherRidesAndItsSuccessorCannotOvertake() {
        row("a", 1, 0); row("b", 1, 1); row("c", 2, 2);
        kafka.failKeys.add("1");
        dispatcher.publishPending();
        assertThat(kafka.keys).containsExactly("1", "2");
        assertThat(read("a").getDeliveryAttempts()).isEqualTo(1);
        assertThat(read("b").getDeliveryAttempts()).isZero();
        assertThat(read("c").getPublishedAt()).isNotNull();
        dispatcher.publishPending();
        assertThat(kafka.keys).hasSize(2);
        kafka.failKeys.clear(); clock.advance(1);
        worker().publishPending();
        assertThat(read("a").getPublishedAt()).isNotNull();
        assertThat(read("b").getPublishedAt()).isNull();
        dispatcher.publishPending();
        assertThat(read("b").getPublishedAt()).isNotNull();
    }

    @Test void retriesAndExponentialBackoffSurviveNewDispatcherInstances() {
        row("a", 1, 0); kafka.failKeys.add("1");
        dispatcher.publishPending();
        assertThat(read("a").getNextAttemptAt()).isEqualTo(clock.instant().plusSeconds(1));
        worker().publishPending(); assertThat(kafka.keys).hasSize(1);
        clock.advance(1); worker().publishPending();
        assertThat(read("a").getDeliveryAttempts()).isEqualTo(2);
        assertThat(read("a").getNextAttemptAt()).isEqualTo(clock.instant().plusSeconds(2));
        clock.advance(1); worker().publishPending(); assertThat(kafka.keys).hasSize(2);
        clock.advance(1); worker().publishPending();
        assertThat(read("a").getNextAttemptAt()).isEqualTo(clock.instant().plusSeconds(4));
        assertThat(read("a").getLastFailureType()).isEqualTo("IllegalStateException");
    }

    @Test void exhaustedEventIsQuarantinedAndReplayResumesItsOrderedStream() {
        row("a", 1, 0); row("b", 1, 1); row("c", 2, 2); kafka.failKeys.add("1");
        for (int i = 0; i < PaymentOutboxDispatcher.MAX_ATTEMPTS; i++) {
            worker().publishPending(); clock.advance(60);
        }
        assertThat(read("a").getDeliveryAttempts()).isEqualTo(8);
        assertThat(read("a").getQuarantinedAt()).isNotNull();
        assertThat(read("a").getPublishedAt()).isNull();
        assertThat(read("b").getDeliveryAttempts()).isZero();
        assertThat(read("c").getPublishedAt()).isNotNull();
        assertThat(repository.findByQuarantinedAtIsNotNullOrderByQuarantinedAtAsc(org.springframework.data.domain.PageRequest.of(0, 20)))
            .extracting(PaymentOutboxEntity::getEventId).containsExactly("a");
        worker().publishPending(); assertThat(kafka.keys.stream().filter("1"::equals)).hasSize(8);
        kafka.failKeys.clear();
        assertThat(dispatcher.replayQuarantined("a")).isTrue();
        assertThat(dispatcher.replayQuarantined("a")).isFalse();
        assertThat(read("a").getDeliveryAttempts()).isZero();
        worker().publishPending(); worker().publishPending();
        assertThat(read("a").getPublishedAt()).isNotNull();
        assertThat(read("b").getPublishedAt()).isNotNull();
    }

    @Test void permanentFailureQuarantinesImmediatelyAndDoesNotStoreSensitiveMessage() {
        row("a", 1, 0); row("b", 2, 1);
        kafka.failure = new SerializationException("secret body must not be persisted"); kafka.failKeys.add("1");
        dispatcher.publishPending();
        assertThat(read("a").getQuarantinedAt()).isNotNull();
        assertThat(read("a").getDeliveryAttempts()).isEqualTo(1);
        assertThat(read("a").getLastFailureType()).isEqualTo("SerializationException");
        assertThat(read("b").getPublishedAt()).isNotNull();
    }

    @Test void pendingAcknowledgementDoesNotHoldDatabaseTransactionOrBlockAnotherRide() {
        row("a", 1, 0);
        // More than a whole candidate batch of successors still consumes only
        // one in-flight slot for this ride. The later unrelated ride is selected.
        for (int i = 0; i < 75; i++) row("same-ride-" + i, 1, i + 1);
        row("b", 2, 100); kafka.pendingKeys.add("1");
        dispatcher.publishPending();
        var leased = read("a");
        assertThat(leased.getPublishedAt()).isNull();
        assertThat(leased.getDeliveryToken()).isNotNull();
        assertThat(leased.getLeaseUntil()).isEqualTo(clock.instant().plusSeconds(30));
        assertThat(read("b").getPublishedAt()).isNotNull();
        worker().publishPending(); assertThat(kafka.keys).containsExactly("1", "2");
        kafka.complete(0);
        assertThat(read("a").getPublishedAt()).isNotNull();
    }

    @Test void expiredClaimRecoversAndLateAcknowledgementCannotOverwriteNewOwner() {
        row("a", 1, 0); kafka.pendingKeys.add("1"); dispatcher.publishPending();
        String oldToken = read("a").getDeliveryToken();
        clock.advance(31); worker().publishPending();
        assertThat(kafka.keys).hasSize(2);
        assertThat(read("a").getDeliveryToken()).isNotEqualTo(oldToken);
        assertThat(read("a").getDeliveryAttempts()).isEqualTo(2);
        kafka.complete(0); assertThat(read("a").getPublishedAt()).isNull();
        kafka.complete(1); assertThat(read("a").getPublishedAt()).isNotNull();
    }

    @Test void finalAttemptProcessCrashIsQuarantinedWithoutExceedingRetryBudget() {
        var fixture = row("a", 1, 0); fixture.setDeliveryAttempts(8);
        fixture.setDeliveryToken("expired"); fixture.setLeaseUntil(clock.instant().minusSeconds(1)); repository.save(fixture);
        dispatcher.publishPending();
        assertThat(kafka.keys).isEmpty();
        assertThat(read("a").getQuarantinedAt()).isNotNull();
        assertThat(read("a").getLastFailureType()).isEqualTo("DeliveryLeaseExpired");
    }

    @Test void equalTimestampsHaveDeterministicPerRideOrdering() {
        row("b", 1, 0); row("a", 1, 0);
        dispatcher.publishPending();
        assertThat(read("b").getDeliverySequence()).isLessThan(read("a").getDeliverySequence());
        assertThat(read("b").getPublishedAt()).isNotNull();
        assertThat(read("a").getPublishedAt()).isNull();
        dispatcher.publishPending(); assertThat(read("a").getPublishedAt()).isNotNull();
    }

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-14T10:00:00Z");
        void advance(long seconds) { now = now.plusSeconds(seconds); }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
    static class RecordingKafka extends KafkaTemplate<String, String> {
        List<String> keys = new ArrayList<>();
        Set<String> failKeys = new HashSet<>(), pendingKeys = new HashSet<>();
        List<CompletableFuture<SendResult<String, String>>> pending = new ArrayList<>();
        Throwable failure = new IllegalStateException("broker unavailable");
        RecordingKafka() { super(new DefaultKafkaProducerFactory<>(Map.of("bootstrap.servers", "127.0.0.1:1"))); }
        @Override public CompletableFuture<SendResult<String, String>> send(String topic, String key, String value) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            keys.add(key);
            if (failKeys.contains(key)) return CompletableFuture.failedFuture(failure);
            if (pendingKeys.contains(key)) {
                var future = new CompletableFuture<SendResult<String, String>>(); pending.add(future); return future;
            }
            return CompletableFuture.completedFuture(result(topic, key));
        }
        void complete(int index) { pending.get(index).complete(result("payment.completed", "1")); }
        static SendResult<String, String> result(String topic, String key) {
            return new SendResult<>(new ProducerRecord<>(topic, key, "{}"),
                new RecordMetadata(new TopicPartition(topic, 0), 0, 0, 0, 0, 0));
        }
    }
}
