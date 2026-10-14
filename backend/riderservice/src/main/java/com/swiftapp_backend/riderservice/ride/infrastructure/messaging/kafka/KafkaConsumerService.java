package com.swiftapp_backend.riderservice.ride.infrastructure.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.swiftapp_backend.riderservice.ride.domain.event.RideDriverAcceptResponseEvent;
import com.swiftapp_backend.riderservice.ride.application.service.RideDriverService;
import com.swiftapp_backend.riderservice.ride.application.service.RideService;
import com.swiftapp_backend.riderservice.shared.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final ObjectMapper objectMapper;
    private final RideDriverService rideDriverService;
    private final RideService rideService;

    @KafkaListener(groupId = "rider-service",topics = {
            "ride-driver-topic",
            "driver.matched",
                "ride.match.failed",
                "driver.arrival.detected"
    })
    public void publish(ConsumerRecord<String,Object> record, Acknowledgment ack) {
        try {
            String topic = record.topic();
            Object data = record.value();
            long offSet = record.offset();
            int partition = record.partition();

            log.debug("Rider event topic={} offset={} partition={}", topic, offSet, partition);

            switch (topic) {
                case "ride-driver-topic" :
                    handleSaveRideDriver(data);
                    break;
                case "driver.matched" :
                    handleMatchedDriver(data);
                    break;
                case "ride.match.failed" :
                    handleMatchFailed(data);
                    break;
                case "driver.arrival.detected" :
                    handleDriverArrivalDetected(data);
                    break;
                case "payment.completed" :
                case "payment-completed" :
                    handlePaymentCompleted(data);
                    break;
            }

            ack.acknowledge();
        }
        catch (Exception e) {
            log.error("Rider event processing failed for topic {}", record.topic(), e);
            throw new IllegalStateException("Rider event processing failed", e);
        }
    }

    @KafkaListener(groupId = "rider-service", topics = {"payment.completed", "payment-completed"},
            containerFactory = "paymentKafkaListenerContainerFactory")
    public void consumePayment(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        publish(record, ack);
    }

    private void handlePaymentCompleted(Object data) {
        JsonNode payload = objectMapper.valueToTree(data);
        for (String field : new String[] {"rideId", "riderId", "driverId", "amount"}) {
            JsonNode value = payload == null ? null : payload.get(field);
            if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0) {
                throw new IllegalArgumentException("Payment " + field + " must be a positive integer");
            }
        }
        PaymentCompletedPayload payment = objectMapper.convertValue(data, PaymentCompletedPayload.class);
        if (!"COMPLETED".equals(payment.paymentStatus()) || payment.rideId() == null
                || payment.riderId() == null || payment.driverId() == null) {
            throw new IllegalArgumentException("Incomplete captured payment event");
        }
        rideService.recordPaymentCompleted(payment.rideId(), payment.riderId(), payment.driverId(),
                payment.amount(), payment.currency(), payment.razorpayPaymentId());
    }


    private void handleSaveRideDriver(Object data) throws Exception {
        if (!(data instanceof java.util.Map<?, ?>)) {
            log.warn("Ignoring ride-driver-topic event with unsupported payload type {}",
                    data == null ? "null" : data.getClass().getSimpleName());
            return;
        }
        RideDriverAcceptResponseEvent rideDriverData;
        try {
            rideDriverData = objectMapper.convertValue(data, RideDriverAcceptResponseEvent.class);
        } catch (IllegalArgumentException ex) {
            log.warn("Ignoring malformed ride-driver-topic event: {}", ex.getMessage());
            return;
        }
        if (rideDriverData.getRideId() == null || rideDriverData.getDriverId() == null
                || rideDriverData.getAuthUserId() == null
                || rideDriverData.getDriverName() == null
                || rideDriverData.getDriverName().isBlank()) {
            log.warn("Ignoring incomplete ride-driver-topic event; rideId={} driverId={} authUserId={}",
                    rideDriverData.getRideId(), rideDriverData.getDriverId(),
                    rideDriverData.getAuthUserId());
            return;
        }
        rideDriverService.saveRideDriver(rideDriverData);
    }

    private void handleMatchedDriver(Object data) {
        if (!(data instanceof java.util.Map<?, ?>)) {
            log.warn("Ignoring driver.matched event with unsupported payload type {}",
                    data == null ? "null" : data.getClass().getSimpleName());
            return;
        }
        MatchedDriverPayload matched;
        try {
            matched = objectMapper.convertValue(data, MatchedDriverPayload.class);
        } catch (IllegalArgumentException ex) {
            log.warn("Ignoring malformed driver.matched event: {}", ex.getMessage());
            return;
        }
        if (matched.rideId() == null || matched.driverId() == null) {
            log.warn("Ignoring driver.matched event missing rideId or driverId");
            return;
        }
        // Matching does not have a verified driver profile snapshot. The ride's
        // driverId and vehicleId are authoritative; a partial ride_driver row is not.
        rideService.assignMatchedDriver(matched.rideId(), matched.driverId(),
                matched.vehicleId());
    }

    private void handleMatchFailed(Object data) {
        if (!(data instanceof java.util.Map<?, ?>)) {
            log.warn("Ignoring ride.match.failed event with unsupported payload type {}",
                    data == null ? "null" : data.getClass().getSimpleName());
            return;
        }
        MatchFailedPayload failed;
        try {
            failed = objectMapper.convertValue(data, MatchFailedPayload.class);
        } catch (IllegalArgumentException ex) {
            log.warn("Ignoring malformed ride.match.failed event: {}", ex.getMessage());
            return;
        }
        if (failed.rideId() == null || failed.rideId() <= 0
                || failed.riderId() == null || failed.riderId() <= 0) {
            log.warn("Ignoring ride.match.failed event missing valid rideId/riderId");
            return;
        }
        try {
            rideService.failMatching(failed.rideId(), failed.riderId(), failed.reason());
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.NOT_FOUND) {
                log.warn("Ignoring stale ride.match.failed event for absent ride {}",
                        failed.rideId());
                return;
            }
            throw ex;
        }
    }

    /**
     * Arrival is detected from the driver's accepted-ride GPS stream. The
     * normal RideService transition remains the single writer for the ride
     * status and publishes the regular driver-arrived event for all clients.
     */
    private void handleDriverArrivalDetected(Object data) {
        if (!(data instanceof java.util.Map<?, ?>)) {
            log.warn("Ignoring driver.arrival.detected with unsupported payload type {}",
                    data == null ? "null" : data.getClass().getSimpleName());
            return;
        }
        DriverArrivalDetectedPayload arrival;
        try {
            arrival = objectMapper.convertValue(data, DriverArrivalDetectedPayload.class);
        } catch (IllegalArgumentException ex) {
            log.warn("Ignoring malformed driver.arrival.detected event: {}", ex.getMessage());
            return;
        }
        if (arrival.rideId() == null || arrival.driverId() == null) {
            log.warn("Ignoring driver.arrival.detected without rideId/driverId");
            return;
        }
        try {
            rideService.driverArrived(arrival.rideId(), arrival.driverId());
        } catch (ApiException ex) {
            // A driver's manual arrival button can race the automatic event,
            // or the ride can already have started. In either case the desired
            // state has already advanced and the event is safely consumed.
            log.info("Ignoring late automatic arrival for ride {}: {}",
                    arrival.rideId(), ex.getMessage());
        }
    }

    private record MatchFailedPayload(Long rideId, Long riderId, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaymentCompletedPayload(Long rideId, Long riderId, Long driverId,
            String paymentStatus, Long amount, String currency, String razorpayPaymentId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DriverArrivalDetectedPayload(Long rideId, Long driverId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MatchedDriverPayload(Long rideId, Long driverId, Long vehicleId) {}
}
