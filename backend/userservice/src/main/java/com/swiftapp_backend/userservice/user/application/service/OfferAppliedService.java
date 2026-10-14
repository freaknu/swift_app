package com.swiftapp_backend.userservice.user.application.service;

import com.swiftapp_backend.userservice.user.domain.event.ApplyOfferRequestEvent;
import com.swiftapp_backend.userservice.user.application.mapper.ApplyOfferMapper;
import com.swiftapp_backend.userservice.user.domain.model.AppliedOfferTrackModel;
import com.swiftapp_backend.userservice.user.domain.repository.OfferAppliedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfferAppliedService {
    private final OfferAppliedRepository offerAppliedRepository;

    @Transactional
    public void handleApplyOffer(ApplyOfferRequestEvent data) {
        AppliedOfferTrackModel newOfferTrack = ApplyOfferMapper.toModel(data);
        offerAppliedRepository.save(newOfferTrack);
    }

    @Transactional(readOnly = true)
    public List<Long> appliedOfferId(Long authUserId) {
        return offerAppliedRepository.findAllByAuthUserId(authUserId)
                .stream()
                .map(AppliedOfferTrackModel::getOfferId)
                .toList();
    }
}
