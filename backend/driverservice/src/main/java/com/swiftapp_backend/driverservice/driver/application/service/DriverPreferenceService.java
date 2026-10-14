package com.swiftapp_backend.driverservice.driver.application.service;

import com.swiftapp_backend.driverservice.driver.api.dto.DriverPreferenceCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverPreferenceResponseDto;
import com.swiftapp_backend.driverservice.driver.application.mapper.DriverPreferenceMapper;
import com.swiftapp_backend.driverservice.driver.application.port.DriverEventPublisherPort;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverPreferenceModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverPreferenceRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DriverPreferenceService {
    private final DriverPreferenceRepository driverPreferenceRepository;
    private final DriverRepository driverRepository;
    private final DriverEventPublisherPort eventPublisher;

    @Transactional
    public DriverPreferenceResponseDto addDriverPreference(DriverPreferenceCreateRequestDto data) throws Exception {
        Long driverId = data.getDriverId();
        DriverModel existingDriver = driverRepository.findById(driverId)
                .orElseThrow(()->new Exception("Driver don't exists"));

        DriverPreferenceModel newPreference = DriverPreferenceModel
                .builder()
                .acceptCash(data.getAcceptCash())
                .acceptOnline(data.getAcceptOnline())
                .autoAccept(data.getAutoAccept())
                .driver(existingDriver)
                .build();
        newPreference = driverPreferenceRepository.save(newPreference);
        var res = DriverPreferenceMapper.toResponseDto(newPreference);
        eventPublisher.publish("driver.preference.updated", "driver" + driverId, res);
        return res;
    }

    @Transactional
    public DriverPreferenceResponseDto updateDriverPreference(DriverPreferenceCreateRequestDto data) throws Exception {
        Long driverId = data.getDriverId();
        DriverPreferenceModel existingDriverPreference = driverPreferenceRepository.findByDriverId(driverId)
                .orElseThrow(()->new Exception("Driver Preference Don't exists"));
        if(data.getAcceptCash() != null) {
            existingDriverPreference.setAcceptCash(data.getAcceptCash());
        }
        if(data.getAcceptOnline() != null) {
            existingDriverPreference.setAcceptOnline(data.getAcceptOnline());
        }
        if(data.getAutoAccept()!= null) {
            existingDriverPreference.setAutoAccept(data.getAutoAccept());
        }

        existingDriverPreference = driverPreferenceRepository.save(existingDriverPreference);
        var res = DriverPreferenceMapper.toResponseDto(existingDriverPreference);
        eventPublisher.publish("driver.preference.updated", "driver" + driverId, res);
        return res;
    }

    @Transactional
    public DriverPreferenceResponseDto getDriverPreference(Long driverId) throws Exception {
        DriverPreferenceModel existingDriverPreference = driverPreferenceRepository.findByDriverId(driverId)
                .orElseThrow(()->new Exception("Driver Preference Don't exists"));
        return DriverPreferenceMapper.toResponseDto(existingDriverPreference);
    }
}
