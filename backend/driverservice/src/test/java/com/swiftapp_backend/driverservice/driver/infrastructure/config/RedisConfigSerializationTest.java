package com.swiftapp_backend.driverservice.driver.infrastructure.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentResponseDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverDeviceInfoRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverNotificationPreferencesDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverResponseDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverSmallDetailDto;
import com.swiftapp_backend.driverservice.driver.api.dto.VehicleResponseDto;
import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverStatus;
import com.swiftapp_backend.driverservice.driver.domain.enums.VehicleType;
import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class RedisConfigSerializationTest {
    @Test
    void immutableVehicleListRoundTripsThroughConfiguredSerializer() {
        RedisSerializer<Object> serializer = serializer();
        List<VehicleResponseDto> vehicles = Stream.of(vehicle()).toList();

        Object decoded = serializer.deserialize(serializer.serialize(vehicles));

        assertInstanceOf(List.class, decoded);
        List<VehicleResponseDto> mapped = new ObjectMapper().findAndRegisterModules()
                .convertValue(decoded, new TypeReference<List<VehicleResponseDto>>() {});
        assertEquals(VehicleType.SEDAN, mapped.getFirst().getVehicleType());
        assertEquals("City", mapped.getFirst().getModel());
        assertNotNull(mapped.getFirst().getCreatedAt());
    }

    @Test
    void profileWithNestedVehicleAndDocumentListsRoundTrips() {
        RedisSerializer<Object> serializer = serializer();
        DriverResponseDto profile = DriverResponseDto.builder()
                .driverId(11L)
                .allVehicles(List.of(vehicle()))
                .allDocuments(List.of(DriverDocumentResponseDto.builder()
                        .id(23L)
                        .driverId(11L)
                        .documentType(DocumentType.DRIVING_LICENCE)
                        .verificationStatus(VerificationStatus.PENDING)
                        .documentUrl("https://example.test/licence")
                        .uploadedAt(LocalDateTime.of(2026, 10, 8, 12, 0))
                        .build()))
                .build();

        Object decoded = serializer.deserialize(serializer.serialize(profile));

        DriverResponseDto mapped = new ObjectMapper().findAndRegisterModules()
                .convertValue(decoded, DriverResponseDto.class);
        assertEquals(11L, mapped.getDriverId());
        assertEquals(VehicleType.SEDAN, mapped.getAllVehicles().getFirst().getVehicleType());
        assertEquals(DocumentType.DRIVING_LICENCE,
                mapped.getAllDocuments().getFirst().getDocumentType());
    }

    @Test
    void otherDriverCacheDtosStillRoundTrip() {
        RedisSerializer<Object> serializer = serializer();
        DriverSmallDetailDto small = DriverSmallDetailDto.builder()
                .driverId(11L).driverStatus(DriverStatus.ONLINE).isVerified(true).build();
        DriverNotificationPreferencesDto preferences = DriverNotificationPreferencesDto.builder()
                .pushEnabled(true).rideAlertsEnabled(true).build();
        DriverDeviceInfoRequestDto device = DriverDeviceInfoRequestDto.builder()
                .deviceToken("token").deviceModel("Phone").build();

        assertEquals(DriverStatus.ONLINE,
                assertInstanceOf(DriverSmallDetailDto.class,
                        serializer.deserialize(serializer.serialize(small))).getDriverStatus());
        assertEquals(true,
                assertInstanceOf(DriverNotificationPreferencesDto.class,
                        serializer.deserialize(serializer.serialize(preferences))).isPushEnabled());
        assertEquals("Phone",
                assertInstanceOf(DriverDeviceInfoRequestDto.class,
                        serializer.deserialize(serializer.serialize(device))).getDeviceModel());
    }

    private static VehicleResponseDto vehicle() {
        return VehicleResponseDto.builder()
                .id(7L)
                .driverId(11L)
                .vehicleType(VehicleType.SEDAN)
                .brand("Honda")
                .model("City")
                .registrationNumber("KA01AB1234")
                .year(2024)
                .createdAt(LocalDateTime.of(2026, 10, 8, 12, 0))
                .build();
    }

    @SuppressWarnings("unchecked")
    private static RedisSerializer<Object> serializer() {
        return (RedisSerializer<Object>) new RedisConfig()
                .redisTemplate(mock(RedisConnectionFactory.class))
                .getValueSerializer();
    }
}
