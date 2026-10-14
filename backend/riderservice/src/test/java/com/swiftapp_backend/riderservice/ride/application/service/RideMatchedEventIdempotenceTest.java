package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideDto;
import com.swiftapp_backend.riderservice.ride.application.mapper.RideMapper;
import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import com.swiftapp_backend.riderservice.ride.domain.model.RideModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.RideRepository;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RideMatchedEventIdempotenceTest {

    @Test
    void sameMatchRedeliveryCannotResetAnAdvancedOrTerminalRide() {
        for (RideStatus status : new RideStatus[] {
                RideStatus.DRIVER_ASSIGNED, RideStatus.DRIVER_ARRIVED,
                RideStatus.STARTED, RideStatus.COMPLETED, RideStatus.CANCELLED}) {
            RideModel ride = RideModel.builder()
                    .id(11L).driverId(22L).vehicleId(33L).currentStatus(status).build();
            RideService service = serviceFor(ride);

            RideDto result = service.assignMatchedDriver(11L, 22L, 33L);

            assertThat(result.getCurrentStatus()).isEqualTo(status);
            assertThat(ride.getCurrentStatus()).isEqualTo(status);
            assertThat(ride.getDriverId()).isEqualTo(22L);
        }
    }

    @Test
    void conflictingMatchCannotReplaceTheAssignedDriverOrVehicle() {
        RideModel ride = RideModel.builder().id(11L).driverId(22L).vehicleId(33L)
                .currentStatus(RideStatus.DRIVER_ARRIVED).build();
        RideService service = serviceFor(ride);

        assertThatThrownBy(() -> service.assignMatchedDriver(11L, 23L, 33L))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.assignMatchedDriver(11L, 22L, 34L))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.assignMatchedDriver(11L, 22L, null))
                .isInstanceOf(ApiException.class);
        assertThat(ride.getDriverId()).isEqualTo(22L);
        assertThat(ride.getVehicleId()).isEqualTo(33L);
    }

    private RideService serviceFor(RideModel ride) {
        RideRepository repository = (RideRepository) Proxy.newProxyInstance(
                RideRepository.class.getClassLoader(), new Class<?>[] {RideRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByIdForUpdate")) {
                        return Optional.of(ride);
                    }
                    throw new AssertionError("Duplicate match tried to call " + method.getName());
                });
        RideMapper mapper = new RideMapper(null, null) {
            @Override
            public RideDto toDto(RideModel model) {
                return RideDto.builder().id(model.getId()).driverId(model.getDriverId())
                        .currentStatus(model.getCurrentStatus()).build();
            }
        };
        return new RideService(repository, null, null, null, null, mapper,
                null, null, null, null);
    }
}
