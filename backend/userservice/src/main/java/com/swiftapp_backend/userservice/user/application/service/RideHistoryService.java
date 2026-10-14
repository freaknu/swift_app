package com.swiftapp_backend.userservice.user.application.service;

import com.swiftapp_backend.userservice.shared.ApiException;
import com.swiftapp_backend.userservice.user.api.dto.RideHistoryResponseDto;
import com.swiftapp_backend.userservice.user.application.mapper.RideHistoryMapper;
import com.swiftapp_backend.userservice.user.application.port.RideHistoryOwnershipPort;
import com.swiftapp_backend.userservice.user.domain.event.RideHistoryCreateEvent;
import com.swiftapp_backend.userservice.user.domain.model.RideHistoryModel;
import com.swiftapp_backend.userservice.user.domain.repository.RideHistoryRepository;
import com.swiftapp_backend.userservice.user.domain.repository.UserModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RideHistoryService {
    private final RideHistoryRepository rideHistoryRepository;
    private final UserModelRepository userModelRepository;
    private final RideHistoryOwnershipPort rideOwnership;

    @Transactional(readOnly = true)
    public List<RideHistoryResponseDto> getRideHistory(Long authUserId) {
        Long profileId = userModelRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> ApiException.notFound("Rider profile not found"))
                .getId();
        List<Long> candidateIds = Objects.equals(authUserId, profileId)
                ? List.of(authUserId) : List.of(authUserId, profileId);
        Map<Long, Optional<Long>> actualRiders = new HashMap<>();
        Set<Long> seenRideIds = new HashSet<>();
        List<RideHistoryResponseDto> owned = new ArrayList<>();
        for (RideHistoryModel row : rideHistoryRepository.findByAuthUserIdInOrderByCreatedAtDesc(candidateIds)) {
            if (row.getRideId() == null) continue;
            // New event projections are authoritative; legacy rows have null here.
            boolean belongsToRider = row.getRiderProfileId() != null
                    ? profileId.equals(row.getRiderProfileId())
                    : actualRiders.computeIfAbsent(row.getRideId(), rideOwnership::riderProfileIdForRide)
                            .filter(profileId::equals).isPresent();
            if (belongsToRider && seenRideIds.add(row.getRideId())) {
                owned.add(RideHistoryMapper.toResponse(row));
            }
        }
        return owned;
    }

    public List<RideHistoryResponseDto> getRideHistory(Long authUserId, int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) throw ApiException.badRequest("Invalid pagination");
        return getRideHistory(authUserId).stream().skip((long) page * pageSize).limit(pageSize).toList();
    }

    @Transactional
    public void addRideHistory(RideHistoryCreateEvent data) {
        RideHistoryModel rideHistory = RideHistoryMapper.toModel(data);
        if (data.getRiderId() != null) {
            rideHistory.setAuthUserId(userModelRepository.findById(data.getRiderId())
                    .orElseThrow(() -> ApiException.notFound("Rider profile not found")).getAuthUserId());
        }
        if (rideHistory.getAuthUserId() == null || data.getRideId() == null) {
            throw ApiException.badRequest("History event is missing its identity");
        }
        if (rideHistoryRepository.findByAuthUserIdOrderByCreatedAtDesc(rideHistory.getAuthUserId())
                .stream().anyMatch(existing -> data.getRideId().equals(existing.getRideId()))) return;
        rideHistoryRepository.save(rideHistory);
    }
}
