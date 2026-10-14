package com.swiftapp_backend.locationservice.location.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.locationservice.location.api.dto.event.LocationUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationRedisSerializationTest {
    @Test
    void gpsTimestampSurvivesProductionSerializerRoundTrip() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);
        Instant when = Instant.parse("2026-10-11T15:00:00Z");
        LocationUpdateDto original = LocationUpdateDto.builder().eventId("fix-1")
                .eventTime(when).driverId(55L).rideId(80L).riderId(91L)
                .latitude(12.97).longitude(77.59).build();

        Object redisValue = serializer.deserialize(serializer.serialize(original));
        LocationUpdateDto restored = mapper.convertValue(redisValue, LocationUpdateDto.class);

        assertEquals(when, restored.getEventTime());
        assertEquals(55L, restored.getDriverId());
        assertEquals(80L, restored.getRideId());
    }
}
