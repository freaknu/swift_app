package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.mapper;

import com.swiftapp_backend.riderservice.ride.domain.enums.ChangedByType;
import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import com.swiftapp_backend.riderservice.ride.domain.model.FareDetailModel;
import com.swiftapp_backend.riderservice.ride.domain.model.RideModel;
import com.swiftapp_backend.riderservice.ride.domain.model.RideStatusHistoryModel;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RidePersistenceMapperTests {

    private final RidePersistenceMapper mapper = new RidePersistenceMapper();

    @Test
    void roundTripPreservesAggregateValuesAndRideBackReferences() {
        RideModel ride = RideModel.builder()
                .id(41L)
                .version(3L)
                .riderId(101L)
                .activeRiderId(101L)
                .currentStatus(RideStatus.MATCHING)
                .build();
        FareDetailModel fare = FareDetailModel.builder()
                .id(51L)
                .totalFare(new BigDecimal("245.50"))
                .ride(ride)
                .build();
        RideStatusHistoryModel history = RideStatusHistoryModel.builder()
                .id(61L)
                .rideStatus(RideStatus.MATCHING)
                .changedByType(ChangedByType.SYSTEM)
                .ride(ride)
                .build();
        ride.setFareDetail(fare);
        ride.setRideHistory(List.of(history));

        RideEntity entity = mapper.toEntity(ride);

        assertThat(entity.getFareDetail().getRide()).isSameAs(entity);
        assertThat(entity.getRideHistory()).singleElement()
                .extracting(item -> item.getRide())
                .isSameAs(entity);

        RideModel roundTripped = mapper.toDomain(entity);

        assertThat(roundTripped.getId()).isEqualTo(41L);
        assertThat(roundTripped.getVersion()).isEqualTo(3L);
        assertThat(roundTripped.getFareDetail().getTotalFare())
                .isEqualByComparingTo("245.50");
        assertThat(roundTripped.getFareDetail().getRide()).isSameAs(roundTripped);
        assertThat(roundTripped.getRideHistory()).singleElement()
                .extracting(RideStatusHistoryModel::getRide)
                .isSameAs(roundTripped);
    }
}
