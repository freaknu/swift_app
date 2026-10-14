package com.swiftapp_backend.userservice.user.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.userservice.user.domain.event.ApplyOfferRequestEvent;
import com.swiftapp_backend.userservice.user.domain.event.RideHistoryCreateEvent;
import com.swiftapp_backend.userservice.user.domain.event.UserCreateEvent;
import com.swiftapp_backend.userservice.user.application.service.OfferAppliedService;
import com.swiftapp_backend.userservice.user.application.service.RideHistoryService;
import com.swiftapp_backend.userservice.user.application.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final RideHistoryService rideHistoryService;
    private final OfferAppliedService offerAppliedService;

    @KafkaListener(
            topics = {
                    "user-created-topic",
                    "user.created",
                    "ride-history-created-topic",
                    "ride-offer-applied-topic"
            },
            groupId = "user-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment
    ) {

        try {
            String topic = record.topic();
            Object data = record.value();

            switch (topic) {

                case "user-created-topic":
                case "user.created":
                    handleSaveUser(data);
                    break;
                case "ride-history-created-topic":
                    handleSaveRideHistory(data);
                    break;
                case "ride-offer-applied-topic":
                    handleOfferApply(data);
                    break;
                default:
                    return;
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            throw e;
        }
    }

    private void handleSaveUser(Object value) {

        UserCreateEvent data =
                objectMapper.convertValue(value, UserCreateEvent.class);
        log.debug("User Create Event {}",data);
        userService.createUserByKafka(data);
    }

    private void handleSaveRideHistory(Object value) {
        RideHistoryCreateEvent data = objectMapper.convertValue(value,RideHistoryCreateEvent.class);
        log.info("Ride History Create Event {}",data);
        rideHistoryService.addRideHistory(data);
    }

    private void handleOfferApply(Object value) {
        ApplyOfferRequestEvent data = objectMapper.convertValue(value,ApplyOfferRequestEvent.class);
        offerAppliedService.handleApplyOffer(data);
    }
}