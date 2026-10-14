package com.swiftapp_backend.locationservice.location.application.service;

import com.swiftapp_backend.locationservice.location.api.dto.event.LocationUpdateDto;
import com.swiftapp_backend.locationservice.location.application.mapper.LocationUpdateMapper;
import com.swiftapp_backend.locationservice.location.domain.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationPersistenceService {

    private final LocationRepository repository;
    private final LocationUpdateMapper mapper;

    @Async
    public void save(LocationUpdateDto dto) {
        repository.save(mapper.toModel(dto));
    }
}
