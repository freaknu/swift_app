package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.api.dto.offer.OfferFetchRequestDto;
import com.swiftapp_backend.riderservice.ride.api.dto.offer.OfferResponseDto;
import com.swiftapp_backend.riderservice.ride.application.mapper.OfferMapper;
import com.swiftapp_backend.riderservice.ride.domain.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OfferService {
    private final OfferRepository offerRepository;


    @Transactional
    public List<OfferResponseDto> getAllOffers(Long authUserId, OfferFetchRequestDto data) {
        LocalDateTime startFrom = data.getStartFrom();

        return offerRepository.findActiveByRideTypeCode(data.getRideTypeCode())
                .stream()
                .map(OfferMapper::toDto)
                .toList();
    }
}
