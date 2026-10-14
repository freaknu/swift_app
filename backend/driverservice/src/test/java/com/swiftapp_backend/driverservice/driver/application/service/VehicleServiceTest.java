package com.swiftapp_backend.driverservice.driver.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.api.dto.VehicleCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.VehicleResponseDto;
import com.swiftapp_backend.driverservice.driver.application.port.DriverCachePort;
import com.swiftapp_backend.driverservice.driver.application.port.DriverEventPublisherPort;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverStatus;
import com.swiftapp_backend.driverservice.driver.domain.enums.VehicleType;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter.DriverRepositoryAdapter;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter.VehicleRepositoryAdapter;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataVehicleRepository;
import com.swiftapp_backend.driverservice.shared.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.redis.serializer.SerializationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test: POST /api/v1/vehicle returned 500 when optional numeric
 * fields (manufactureYear, seatCapacity, seats) were absent from the payload.
 * VehicleMapper.toModel unboxed null Integers into int fields (NPE).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VehicleServiceTest {

    @Autowired
    private SpringDataVehicleRepository springDataVehicleRepository;
    @Autowired
    private SpringDataDriverRepository springDataDriverRepository;

    private VehicleService service;
    private DriverCachePort cache;

    @BeforeEach
    void setUp() {
        DriverRepositoryAdapter driverAdapter = new DriverRepositoryAdapter(springDataDriverRepository);
        DriverModel driver = driverAdapter.save(DriverModel.builder()
                .authUserId(32L)
                .driverName("Edfffff")
                .email("Pk22tyy39.29.jnv@gmail.com")
                .phoneNumber("8558825555")
                .driverStatus(DriverStatus.OFFLINE)
                .isActive(true)
                .isAcceptingRides(false)
                .isVerified(false)
                .build());
        assertNotNull(driver.getId());

        VehicleRepositoryAdapter vehicleAdapter =
                new VehicleRepositoryAdapter(springDataVehicleRepository, springDataDriverRepository);
        cache = mock(DriverCachePort.class);
        service = new VehicleService(
                vehicleAdapter,
                driverAdapter,
                cache,
                new ObjectMapper(),
                mock(DriverEventPublisherPort.class));
    }

    @Test
    void addVehicle_withOnlyRequiredFields_succeedsAndDefaultsNumerics() throws Exception {
        VehicleCreateRequestDto dto = VehicleCreateRequestDto.builder()
                .vehicleType(VehicleType.BIKE)
                .brand("Ghehw")
                .model("Gwgw")
                .registrationNumber("WHGWVEBEHEB")
                .year(2003)
                .color("Blue")
                .build();

        VehicleResponseDto response = service.addVehicle(32L, dto);

        assertNotNull(response.getId());
        assertEquals(VehicleType.BIKE, response.getVehicleType());
        assertEquals("WHGWVEBEHEB", response.getRegistrationNumber());
        assertEquals(0, response.getManufactureYear());
        assertEquals(0, response.getSeatCapacity());
        assertEquals(0, response.getSeats());
    }

    @Test
    void updateVehiclePersistsEditsWithoutCreatingDuplicate() throws Exception {
        VehicleCreateRequestDto original = vehicle("DL01AB1234");
        VehicleResponseDto created = service.addVehicle(32L, original);
        VehicleCreateRequestDto edits = vehicle("DL01AB1234");
        edits.setModel("City Hybrid");
        edits.setYear(2024);
        edits.setColor("White");

        VehicleResponseDto updated = service.updateVehicle(32L, created.getId(), edits);

        assertEquals(created.getId(), updated.getId());
        assertEquals("City Hybrid", updated.getModel());
        assertEquals(2024, updated.getYear());
        assertEquals("White", updated.getColor());
        assertEquals(1, springDataVehicleRepository.findAllByAuthUserId(32L).size());
        assertEquals("City Hybrid", service.getAllVehicle(32L).getFirst().getModel());
    }

    @Test
    void updateRejectsOtherDriverAndDuplicateRegistration() throws Exception {
        VehicleResponseDto first = service.addVehicle(32L, vehicle("DL01AB1234"));
        service.addVehicle(32L, vehicle("DL01AB5678"));

        assertThrows(ApiException.class,
                () -> service.updateVehicle(99L, first.getId(), vehicle("DL01AB1234")));
        assertThrows(ApiException.class,
                () -> service.addVehicle(32L, vehicle("DL01AB1234")));
        assertThrows(ApiException.class,
                () -> service.updateVehicle(32L, first.getId(), vehicle("DL01AB5678")));
        assertEquals(2, springDataVehicleRepository.findAllByAuthUserId(32L).size());
    }

    @Test
    void vehicleTypeOptionsMatchEverySupportedEnum() {
        var controller = new com.swiftapp_backend.driverservice.driver.api.controller.VehicleController(service);
        var options = controller.getVehicleTypes().getBody().getData();

        assertEquals(VehicleType.values().length, options.size());
        assertTrue(options.stream().anyMatch(option -> option.code().equals("AUTO_RICKSHAW")
                && option.name().equals("Auto Rickshaw")));
    }

    @Test
    void unreadableLegacyVehicleCacheIsEvictedAndReadThrough() {
        try {
            service.addVehicle(32L, vehicle("DL01AB1234"));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
        when(cache.exists("driverVehicles32")).thenReturn(true);
        when(cache.get("driverVehicles32")).thenThrow(new SerializationException("legacy payload"));

        assertEquals(1, service.getAllVehicle(32L).size());
        verify(cache, org.mockito.Mockito.atLeastOnce()).delete("driverVehicles32");
    }

    private static VehicleCreateRequestDto vehicle(String registrationNumber) {
        return VehicleCreateRequestDto.builder()
                .vehicleType(VehicleType.SEDAN)
                .brand("Honda")
                .model("City")
                .year(2022)
                .registrationNumber(registrationNumber)
                .build();
    }
}
