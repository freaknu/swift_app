package com.swiftapp_backend.matchingservice.matching.infrastructure.messaging.kafka;

import com.swiftapp_backend.matchingservice.common.messaging.kafka.topics.KafkaTopic;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverMatchedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideOfferBroadcastEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideOfferWithdrawnEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideMatchFailedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverOfferRepository;
import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingStateRepository;
import com.swiftapp_backend.matchingservice.matching.application.port.MatchingEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * Publishes matching lifecycle events for riderservice, driverservice
 * and the websocket layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingEventPublisher implements MatchingEventPublisherPort {

    private final KafkaTemplate<String, Object> matchingKafkaTemplate;
    private final DriverOfferRepository offerRepository;
    private final MatchingStateRepository stateRepository;

    public void publishDriverMatched(DriverMatchedEvent event) {
        sendMatched(event);
    }

    /** Replays events whose Redis accept CAS succeeded but Kafka has not acked. */
    @Scheduled(fixedDelayString = "${matching.outbox-retry-interval-ms:2000}")
    public void retryPendingMatchedEvents() {
        try {
            offerRepository.pendingMatchedEvents(100).forEach(this::sendMatched);
        } catch (Exception ex) {
            log.error("Could not scan driver.matched outbox", ex);
        }
    }

    /** Retries no-driver outcomes until both rider and socket consumers can receive them. */
    @Scheduled(fixedDelayString = "${matching.outbox-retry-interval-ms:2000}")
    public void retryPendingFailedEvents() {
        try {
            stateRepository.pendingFailedEvents(100).forEach(this::sendFailed);
        } catch (Exception ex) {
            log.error("Could not scan ride.match.failed outbox", ex);
        }
    }

    private void sendMatched(DriverMatchedEvent event) {
        if (event == null || event.getRideId() == null) return;
        try {
            matchingKafkaTemplate.send(KafkaTopic.DRIVER_MATCHED,
                    event.getRideId().toString(), event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("driver.matched delivery failed for ride {}; queued for retry",
                            event.getRideId(), ex);
                    return;
                }
                try {
                    offerRepository.acknowledgeMatchedEvent(event.getRideId());
                } catch (Exception ackError) {
                    // A duplicate on the next retry is safe: riderservice assignment
                    // is idempotent for the same driver and ride.
                    log.error("driver.matched sent but outbox ack failed for ride {}",
                            event.getRideId(), ackError);
                }
            });
        } catch (Exception ex) {
            log.error("driver.matched send unavailable for ride {}; queued for retry",
                    event.getRideId(), ex);
        }
    }

    /**
     * One matching attempt, delivered to its single selected driver through
     * the websocket service and staged in the driver's pending-offer inbox.
     */
    public void publishOfferBroadcast(RideOfferBroadcastEvent event) {
        send(KafkaTopic.RIDE_OFFER_BROADCAST,
                event.getRideId() != null ? event.getRideId().toString() : null,
                event);
    }

    /**
     * Offers that stopped being actionable for the listed drivers.
     */
    public void publishOfferWithdrawn(Long rideId, List<Long> driverIds, String reason) {
        RideOfferWithdrawnEvent event = RideOfferWithdrawnEvent.builder()
                .rideId(rideId)
                .driverIds(driverIds)
                .reason(reason)
                .build();
        send(KafkaTopic.RIDE_OFFER_WITHDRAWN,
                rideId != null ? rideId.toString() : null,
                event);
    }

    public void publishMatchFailed(Long rideId, Long riderId, String reason) {
        sendFailed(new RideMatchFailedEvent(rideId, riderId, reason));
    }

    private void sendFailed(RideMatchFailedEvent event) {
        if (event == null || event.rideId() == null) return;
        try {
            matchingKafkaTemplate.send(KafkaTopic.RIDE_MATCH_FAILED,
                    event.rideId().toString(), event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("ride.match.failed delivery failed for ride {}; queued for retry",
                            event.rideId(), ex);
                    return;
                }
                try {
                    stateRepository.acknowledgeFailedEvent(event.rideId());
                } catch (Exception ackError) {
                    log.error("ride.match.failed sent but outbox ack failed for ride {}",
                            event.rideId(), ackError);
                }
            });
        } catch (Exception ex) {
            log.error("ride.match.failed send unavailable for ride {}; queued for retry",
                    event.rideId(), ex);
        }
    }

    private void send(String topic, String key, Object payload) {
        // Publishing must never break the matching flow: Redis holds the
        // source of truth, Kafka is a notification. If the broker is down,
        // log and continue - the offer/timeout state in Redis keeps working.
        try {
            matchingKafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish to topic {}", topic, ex);
                } else {
                    log.debug("Published to topic {} (offset {})",
                            topic, result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.error("Kafka unavailable, skipping publish to topic {}", topic, ex);
        }
    }
}
