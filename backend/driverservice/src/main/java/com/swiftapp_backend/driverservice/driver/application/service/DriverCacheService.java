package com.swiftapp_backend.driverservice.driver.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverSmallDetailDto;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverStatus;
import com.swiftapp_backend.driverservice.driver.application.mapper.DriverRedisMapper;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.application.port.DriverCachePort;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverCacheService {
    private final DriverCachePort driverCache;
    private final ObjectMapper objectMapper;
    private final DriverRepository driverRepository;

    private String getDriverSmallDetailKey(Long userId) {
        return "driverMiniDetail"+userId;
    }


    @Async
    public void cacheDriverSummary(DriverModel data) {
        String key = getDriverSmallDetailKey(data.getAuthUserId());

        if(driverCache.exists(key)) {
            driverCache.delete(key);
        }
        DriverSmallDetailDto driverMiniDetail = DriverRedisMapper.toDriverSmallDto(data);
        driverCache.set(key,driverMiniDetail);
    }

    @Async
    public void cacheStatusChange(Long userId,DriverStatus driverStatus) throws Exception {
        log.info("under the status change");
        String key = getDriverSmallDetailKey(userId);
        if(driverCache.exists(key)) {
            DriverSmallDetailDto driverMiniData = objectMapper.convertValue(driverCache.get(key),DriverSmallDetailDto.class);
            driverCache.delete(key);
            log.info("current driver status is {} and after changed {}",driverMiniData.getDriverStatus(),driverStatus);
            driverMiniData.setDriverStatus(driverStatus);
            driverCache.set(key,driverMiniData);
        }

        Optional<DriverModel> existingDriver = driverRepository.findByAuthUserId(userId);
        if(existingDriver.isEmpty()) throw new Exception("Driver detail is not Present");

        DriverSmallDetailDto data = DriverRedisMapper.toDriverSmallDto(existingDriver.get());
        driverCache.set(key,data);
    }

    @Async
    public CompletableFuture<DriverSmallDetailDto> getMiniDriverData(Long userId) throws Exception {
        String key = getDriverSmallDetailKey(userId);
        if(driverCache.exists(key)) {
            return CompletableFuture.completedFuture(objectMapper.convertValue(driverCache.get(key),DriverSmallDetailDto.class));
        }
        Optional<DriverModel> existingDriver = driverRepository.findByAuthUserId(userId);
        if(existingDriver.isEmpty()) throw new Exception("Driver detail is not Present");

        DriverSmallDetailDto data = DriverRedisMapper.toDriverSmallDto(existingDriver.get());
        driverCache.set(key,data);
        return CompletableFuture.completedFuture(data);
    }
}
