package com.swiftapp_backend.riderservice.ride.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaRideCompletionOutboxRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Opt-in real broker smoke test; no local Kafka, user database, provider or credentials. */
@EnabledIfSystemProperty(named = "swift.qa.broker", matches = "true")
@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:ridebrokeroutbox;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.kafka.listener.auto-startup=false",
    "ride.completion-outbox.enabled=false"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({KafkaProducerConfig.class, KafkaProducerService.class, RideCompletionOutbox.class,
    RideCompletionOutboxBrokerTest.JsonConfig.class})
@EmbeddedKafka(kraft = true, partitions = 1, topics = "ride.completed",
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RideCompletionOutboxBrokerTest {
    @TestConfiguration
    static class JsonConfig {
        @Bean ObjectMapper mapper() { return new ObjectMapper().findAndRegisterModules(); }
    }

    @Autowired RideCompletionOutbox outbox;
    @Autowired KafkaProducerService publisher;
    @Autowired JpaRideCompletionOutboxRepository repository;
    @Autowired PlatformTransactionManager manager;
    @Autowired ObjectMapper mapper;
    @Autowired EmbeddedKafkaBroker broker;

    @BeforeEach void clean() { repository.deleteAll(); }

    @Test void rolledBackRideEmitsNothingAndCommittedCanonicalPayloadReachesBrokerBeforeRemoval() throws Exception {
        Map<String, Object> payload = Map.of("rideId", 15, "riderId", 16, "driverId", 17,
            "totalFare", new BigDecimal("123.45"), "currency", "INR",
            "completedAt", "2026-10-14T10:00:00Z");
        var transaction = new TransactionTemplate(manager);
        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps("outbox-" + UUID.randomUUID(), "false", broker);
        consumerProperties.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProperties,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, "ride.completed");
            transaction.executeWithoutResult(status -> {
                publisher.publish("ride.completed", "15", payload);
                assertThat(repository.count()).isEqualTo(1);
                status.setRollbackOnly();
            });
            assertThat(repository.count()).isZero();
            outbox.flush();
            assertThat(consumer.poll(Duration.ofMillis(200))).isEmpty();

            transaction.executeWithoutResult(status -> publisher.publish("ride.completed", "15", payload));
            assertThat(repository.count()).isEqualTo(1);
            outbox.flush();
            var record = KafkaTestUtils.getSingleRecord(consumer, "ride.completed", Duration.ofSeconds(10));
            assertThat(record.key()).isEqualTo("15");
            var delivered = mapper.readTree(record.value());
            assertThat(delivered.path("rideId").asLong()).isEqualTo(15);
            assertThat(delivered.path("riderId").asLong()).isEqualTo(16);
            assertThat(delivered.path("driverId").asLong()).isEqualTo(17);
            assertThat(delivered.path("totalFare").decimalValue()).isEqualByComparingTo("123.45");
            assertThat(delivered.path("currency").asText()).isEqualTo("INR");
            assertThat(delivered.path("completedAt").asText()).isEqualTo("2026-10-14T10:00:00Z");
            // KafkaTemplate acknowledgement invokes the real short JPA completion transaction.
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(repository.count()).isZero());
        }
    }

    @Test void unreachableFreshLoopbackBrokerLeavesACommittedDurableRetryWithoutSleeping() throws Exception {
        final int unavailablePort;
        // Reserve and immediately close a fresh ephemeral loopback endpoint; do
        // not contact the user's configured Kafka or mutate the embedded broker.
        try (ServerSocket unused = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            unavailablePort = unused.getLocalPort();
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:" + unavailablePort);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 500);
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 300);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 1000);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        properties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        var factory = new DefaultKafkaProducerFactory<String, Object>(properties);
        factory.setPhysicalCloseTimeout(1);
        var kafka = new KafkaTemplate<>(factory);
        var attemptTime = Instant.parse("2026-10-14T10:00:00Z");
        try {
            var isolatedWorker = new RideCompletionOutbox(repository, mapper, kafka, manager,
                java.time.Clock.fixed(attemptTime, java.time.ZoneOffset.UTC));
            new TransactionTemplate(manager).executeWithoutResult(status ->
                publisher.publish("ride.completed", "27", Map.of("rideId", 27, "totalFare", "123.45", "currency", "INR")));
            isolatedWorker.flush();
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
                var pending = repository.findAll().getFirst();
                assertThat(pending.getDeliveryAttempts()).isEqualTo(1);
                assertThat(pending.getNextAttemptAt()).isEqualTo(attemptTime.plusSeconds(1));
                assertThat(pending.getDeliveryToken()).isNull();
                assertThat(pending.getQuarantinedAt()).isNull();
                assertThat(pending.getLastFailureType()).isEqualTo("TimeoutException");
            });
            isolatedWorker.flush();
            assertThat(repository.findAll().getFirst().getDeliveryAttempts()).isEqualTo(1);
        } finally {
            kafka.destroy();
            factory.destroy();
        }
    }
}
