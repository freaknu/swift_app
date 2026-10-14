package com.swiftapp_backend.userservice.user.infrastructure.persistence.repository;

import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.AppliedOfferTrackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataOfferAppliedRepository extends JpaRepository<AppliedOfferTrackEntity, Long> {
    List<AppliedOfferTrackEntity> findAllByAuthUserId(Long authUserId);

    List<AppliedOfferTrackEntity> findAllByOfferId(Long offerId);

    List<AppliedOfferTrackEntity> findAllByOfferIdAndAuthUserId(Long offerId, Long authUserId);

    Integer countByOfferIdAndAuthUserId(Long offerId, Long authUserId);
}
