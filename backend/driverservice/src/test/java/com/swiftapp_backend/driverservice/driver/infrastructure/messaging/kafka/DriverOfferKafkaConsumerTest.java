package com.swiftapp_backend.driverservice.driver.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.application.service.DriverOfferService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DriverOfferKafkaConsumerTest {
    @Test
    void headerlessBroadcastStagesCanonicalVehicleTypeForDriverInbox() {
        DriverOfferService offers = mock(DriverOfferService.class);
        Acknowledgment ack = mock(Acknowledgment.class);
        DriverOfferKafkaConsumer consumer = new DriverOfferKafkaConsumer(offers, new ObjectMapper());
        String json = """
                {"rideId":101,"riderId":501,"vehicleType":"HATCHBACK",
                 "pickupLatitude":12.1,"pickupLongitude":77.1,
                 "destinationLatitude":12.2,"destinationLongitude":77.2,
                 "estimatedDistance":4.5,"estimatedDuration":600,
                 "totalFare":"120.00","currency":"INR",
                 "expiresAt":"2030-01-01T00:00:00Z",
                 "targets":[{"driverId":11,"distanceKm":0.4}]}
                """;

        consumer.consumeBroadcast(new ConsumerRecord<>("ride.offer.broadcast", 0, 0,
                "ride-101", json), ack);

        verify(offers).savePendingOffer(11L, 101L, 501L, 0.4,
                "2030-01-01T00:00:00Z", 12.1, 77.1, 12.2, 77.2,
                4.5, 600L, new BigDecimal("120.00"), "INR", "HATCHBACK");
        verify(ack).acknowledge();
    }
}
