package com.swiftapp_backend.driverservice.driver.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.api.dto.VehicleCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.VehicleResponseDto;
import com.swiftapp_backend.driverservice.driver.application.mapper.VehicleMapper;
import com.swiftapp_backend.driverservice.driver.application.port.DriverCachePort;
import com.swiftapp_backend.driverservice.driver.application.port.DriverEventPublisherPort;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.VehicleRepository;
import com.swiftapp_backend.driverservice.shared.common.ApiException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final DriverCachePort driverCache;
    private final ObjectMapper objectMapper;
    private final DriverEventPublisherPort eventPublisher;

    @Transactional
    public VehicleResponseDto addVehicle(Long authUserId, VehicleCreateRequestDto dto) {
        DriverModel driver = driverRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> ApiException.notFound("Driver not found"));
        requireUniqueRegistration(authUserId, dto.getRegistrationNumber(), null);

        VehicleModel vehicle = VehicleMapper.toModel(dto);
        vehicle.setDriver(driver);
        vehicle.setAuthUserId(authUserId);
        vehicle.setActive(true);
        VehicleModel saved = vehicleRepository.save(vehicle);

        invalidateProfileAndVehicles(authUserId);

        eventPublisher.publish("driver.vehicle.updated", "driver" + authUserId, VehicleMapper.toVehicleResponse(saved));

        return VehicleMapper.toVehicleResponse(saved);
    }

    @Transactional
    public VehicleResponseDto updateVehicle(Long authUserId, Long vehicleId,
                                            VehicleCreateRequestDto dto) {
        VehicleModel existing = vehicleRepository.findByAuthUserIdAndId(authUserId, vehicleId)
                .filter(VehicleModel::isActive)
                .orElseThrow(() -> ApiException.notFound("Vehicle not found"));
        requireUniqueRegistration(authUserId, dto.getRegistrationNumber(), vehicleId);
        existing.setVehicleType(dto.getVehicleType());
        existing.setBrand(dto.getBrand());
        existing.setModel(dto.getModel());
        existing.setYear(dto.getYear());
        existing.setRegistrationNumber(dto.getRegistrationNumber());
        if (dto.getFuelType() != null) existing.setFuelType(dto.getFuelType());
        if (dto.getColor() != null) existing.setColor(dto.getColor());
        if (dto.getSeatCapacity() != null) existing.setSeatCapacity(dto.getSeatCapacity());
        if (dto.getIsEv() != null) existing.setEv(dto.getIsEv());
        if (dto.getManufactureYear() != null) existing.setManufactureYear(dto.getManufactureYear());
        if (dto.getSeats() != null) existing.setSeats(dto.getSeats());
        if (dto.getVehicleImage() != null) existing.setVehicleImage(dto.getVehicleImage());

        VehicleModel saved = vehicleRepository.save(existing);
        invalidateProfileAndVehicles(authUserId);
        VehicleResponseDto response = VehicleMapper.toVehicleResponse(saved);
        eventPublisher.publish("driver.vehicle.updated", "driver" + authUserId, response);
        return response;
    }

    @Transactional
    public void deactivateVehicle(Long authUserId,Long vehicleId)throws Exception {
        Optional<VehicleModel> existingVehicle = vehicleRepository.findByAuthUserIdAndId(authUserId,vehicleId);
        if(existingVehicle.isEmpty()) throw new Exception("Vehicle Not found .");
        existingVehicle.get().setActive(false);
        vehicleRepository.save(existingVehicle.get());
        invalidateProfileAndVehicles(authUserId);
        eventPublisher.publish("driver.vehicle.updated", "driver" + authUserId, "Vehicle deactivated: " + vehicleId);
    }

    @Transactional
    public void verifyVehicle(Long authUserId,Long vehicleId) throws Exception {
        Optional<VehicleModel> existingVehicle = vehicleRepository.findByAuthUserIdAndId(authUserId,vehicleId);
        if(existingVehicle.isEmpty()) throw new Exception("Vehicle Not found .");
        boolean alreadyVerified = Boolean.TRUE.equals(existingVehicle.get().getIsVerified());
        Long driverId = alreadyVerified ? null : driverRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> ApiException.notFound("Driver not found")).getId();
        existingVehicle.get().setIsVerified(true);
        vehicleRepository.save(existingVehicle.get());
        invalidateProfileAndVehicles(authUserId);
        eventPublisher.publish("driver.vehicle.updated", "driver" + authUserId, "Vehicle verified: " + vehicleId);
        if (!alreadyVerified) {
            eventPublisher.publish("driver.vehicle.reviewed", "driver" + authUserId,
                    Map.of("eventId", UUID.randomUUID().toString(),
                            "driverId", driverId,
                            "authUserId", authUserId,
                            "status", "VERIFIED",
                            "eventTime", Instant.now().toString(),
                            "vehicleId", vehicleId,
                            "reason", "Vehicle verified"));
        }
    }

    public List<VehicleResponseDto> getAllVehicle(Long authUserId) {
        String key = "driverVehicles" + authUserId;
        if(driverCache.exists(key)) {
            try {
                Object cached = driverCache.get(key);
                if (cached != null) {
                    List<VehicleResponseDto> vehicles = objectMapper.convertValue(cached,
                            new TypeReference<List<VehicleResponseDto>>() {});
                    if (vehicles != null) return vehicles;
                }
            } catch (RuntimeException ex) {
                log.warn("Discarding unreadable vehicle cache for user {}", authUserId, ex);
                driverCache.delete(key);
            }
        }
        List<VehicleModel> allVehicles = vehicleRepository.findAllByAuthUserId(authUserId);
        List<VehicleResponseDto> res = allVehicles
                .stream()
                .filter(VehicleModel::isActive)
                .map(VehicleMapper::toVehicleResponse)
                .toList();

        driverCache.set(key,res,20L, TimeUnit.SECONDS);
        return res;
    }

    private void requireUniqueRegistration(Long authUserId, String registrationNumber,
                                           Long allowedVehicleId) {
        boolean duplicate = vehicleRepository.findAllByAuthUserId(authUserId).stream()
                .anyMatch(vehicle -> vehicle.isActive()
                        && !Objects.equals(vehicle.getId(), allowedVehicleId)
                        && registrationNumber != null
                        && registrationNumber.equalsIgnoreCase(vehicle.getRegistrationNumber()));
        if (duplicate) {
            throw ApiException.conflict("An active vehicle with this registration number already exists");
        }
    }

    private void invalidateProfileAndVehicles(Long authUserId) {
        driverCache.delete("driverVehicles" + authUserId);
        driverCache.delete("driver" + authUserId);
    }


}
