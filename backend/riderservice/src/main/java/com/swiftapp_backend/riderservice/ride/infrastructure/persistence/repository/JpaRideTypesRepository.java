package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository;

import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideTypesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface JpaRideTypesRepository extends JpaRepository<RideTypesEntity, Long> {

    List<RideTypesEntity> findByCodeIn(Collection<String> codes);
    List<RideTypesEntity> findByActiveTrueOrderBySortOrderAsc();
}
