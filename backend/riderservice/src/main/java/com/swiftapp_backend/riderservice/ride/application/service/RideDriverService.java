package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.domain.event.RideDriverAcceptResponseEvent;
import com.swiftapp_backend.riderservice.ride.application.mapper.RideDriverMapper;
import com.swiftapp_backend.riderservice.ride.domain.model.RideDriverModel;
import com.swiftapp_backend.riderservice.ride.domain.model.RideModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.RideDriverRepository;
import com.swiftapp_backend.riderservice.ride.domain.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideDriverService {
    private final RideDriverRepository rideDriverRepository;
    private final RideRepository rideRepository;

    @Transactional
    public void saveRideDriver(RideDriverAcceptResponseEvent data)throws Exception {
        Long rideId = data.getRideId();
        RideModel existingRide = getExistingRide(rideId);
        handleRideDriverDelete(rideId);
        RideDriverModel rideDriver = RideDriverMapper.toModel(data);
        rideDriver.setRide(existingRide);
        rideDriver = rideDriverRepository.save(rideDriver);
        log.info("[Ride Driver Saved] data = {}",rideDriver);
    }


    // Ride Driver Reusable Utility
    private void deleteRideDriver(RideDriverModel data) {
        rideDriverRepository.delete(data);
    }
    private RideModel getExistingRide(Long rideId)throws Exception {
        return rideRepository.findById(rideId)
                .orElseThrow(()->new Exception("Ride Not found"));
    }
    private void handleRideDriverDelete(Long rideId) {
        rideDriverRepository.findByRideId(rideId).ifPresent(this::deleteRideDriver);
    }
}
