package com.swiftapp_backend.userservice.user.domain.repository;

import com.swiftapp_backend.userservice.user.domain.model.RideHistoryModel;

import java.util.List;

public interface RideHistoryRepository {
    RideHistoryModel save(RideHistoryModel rideHistory);

    List<RideHistoryModel> findByAuthUserIdOrderByCreatedAtDesc(Long authUserId);

    List<RideHistoryModel> findByAuthUserIdInOrderByCreatedAtDesc(List<Long> authUserIds);
}
