package com.swiftapp_backend.locationservice.location.application.service;

import com.swiftapp_backend.locationservice.location.api.dto.driver.NearbyDriverDto;
import com.swiftapp_backend.locationservice.location.api.dto.event.DriverFindRequestDto;
import com.swiftapp_backend.locationservice.location.application.port.LocationEventPublisher;
import com.swiftapp_backend.locationservice.location.domain.events.LocationEventUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverMatchService {
    private final LocationService locationService;
    private final LocationEventPublisher eventPublisher;
    public void matchDriver(DriverFindRequestDto req) {
        List<NearbyDriverDto> tempDrivers = locationService.getAllDrivers(req.getLatitude(),req.getLongitude());
        log.info(
                "Matched {} nearby drivers for latitude={}, longitude={}",
                tempDrivers.size(),
                req.getLatitude(),
                req.getLongitude()
        );
        String topic = "driver-broadcast-topic";
        eventPublisher.publish(topic,"driver-broadcast",tempDrivers);
    }
}
