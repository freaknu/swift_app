package com.swiftapp_backend.matchingservice.matching.infrastructure.messaging.kafka;

import com.swiftapp_backend.matchingservice.common.messaging.kafka.topics.KafkaTopic;
import com.swiftapp_backend.matchingservice.matching.application.port.MatchingModuleApi;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverLocationEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverOfferAcceptedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverOfferRejectedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideRequestedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumer {

    private final MatchingModuleApi matchingModuleApi;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {
                    KafkaTopic.DRIVER_LOCATION,
                    "driver.matching.status",
                    "ride.cancelled",
                    KafkaTopic.REQUEST_RIDE
            },
            groupId = "matching-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            ConsumerRecord<String, String> record,
            Acknowledgment ack
    ) {

        try {

            String topic = record.topic();
            String data = record.value();

            log.info(
                    "Received Kafka message | topic={} | partition={} | offset={}",
                    topic,
                    record.partition(),
                    record.offset()
            );

            switch (topic) {

                case KafkaTopic.DRIVER_LOCATION -> {

                    DriverLocationEvent event =
                            objectMapper.readValue(
                                    data,
                                    DriverLocationEvent.class
                            );

                    matchingModuleApi.saveLocation(event);

                    ack.acknowledge();

                    log.info(
                            "LOCATION SAVED INSIDE MATCHING SERVICE: {}",
                            event
                    );
                }
                case "driver.matching.status" -> {
                    java.util.Map<?, ?> payload = objectMapper.readValue(data, java.util.Map.class);
                    Object rawId = payload.get("driverId");
                    Object rawStatus = payload.get("status");
                    if (rawId != null && rawStatus != null) {
                        matchingModuleApi.updateDriverStatus(
                                Long.valueOf(rawId.toString()),
                                DriverStatusEventEnum.valueOf(rawStatus.toString()));
                    }
                    ack.acknowledge();
                }
                case "ride.cancelled" -> {
                    java.util.Map<?, ?> payload = objectMapper.readValue(data, java.util.Map.class);
                    Object rawId = payload.get("rideId");
                    if (rawId != null) {
                        matchingModuleApi.recordRideCancelled(Long.valueOf(rawId.toString()));
                    }
                    ack.acknowledge();
                }
                case KafkaTopic.REQUEST_RIDE -> {
                    matchingModuleApi.requestRide(data);
                    ack.acknowledge();
                    log.info("Ride requested successfully : {}",data);
                }
                default -> {
                    log.warn("Unknown Kafka topic: {}", topic);
                    ack.acknowledge();
                }
            }

        } catch (Exception ex) {

            log.error(
                    "ERROR WHILE CONSUMING KAFKA MESSAGE | topic={} | partition={} | offset={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    ex
            );
        }
    }

    @KafkaListener(
            topics = KafkaTopic.RIDE_REQUESTED,
            groupId = "matching-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeRideRequested(
            ConsumerRecord<String, String> record,
            Acknowledgment ack
    ) {

        try {

            RideRequestedEvent event =
                    objectMapper.readValue(record.value(), RideRequestedEvent.class);

            matchingModuleApi.handleRideRequested(event);

            ack.acknowledge();

            log.info(
                    "Ride request handed to matching engine: rideId={}",
                    event.getRideId()
            );

        } catch (Exception ex) {

            log.error(
                    "ERROR WHILE CONSUMING ride.requested | partition={} | offset={}",
                    record.partition(),
                    record.offset(),
                    ex
            );
        }
    }

    @KafkaListener(
            topics = {
                    KafkaTopic.DRIVER_OFFER_ACCEPTED,
                    KafkaTopic.DRIVER_OFFER_REJECTED
            },
            groupId = "matching-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDriverOfferResponse(
            ConsumerRecord<String, String> record,
            Acknowledgment ack
    ) {

        try {

            String topic = record.topic();
            String data = record.value();

            if (KafkaTopic.DRIVER_OFFER_ACCEPTED.equals(topic)) {

                DriverOfferAcceptedEvent event =
                        objectMapper.readValue(
                                data,
                                DriverOfferAcceptedEvent.class
                        );

                matchingModuleApi.handleDriverOfferAccepted(event);

            } else {

                DriverOfferRejectedEvent event =
                        objectMapper.readValue(
                                data,
                                DriverOfferRejectedEvent.class
                        );

                matchingModuleApi.handleDriverOfferRejected(event);
            }

            ack.acknowledge();

            log.info(
                    "Driver offer response processed: topic={} data={}",
                    topic,
                    data
            );

        } catch (Exception ex) {

            log.error(
                    "ERROR WHILE CONSUMING driver offer response | topic={} | partition={} | offset={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    ex
            );
        }
    }
}
