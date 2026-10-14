package com.swiftapp_backend.matchingservice.matching.application.service;

import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingTimeoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Timeout worker. Offer expiries live in the {@code matching:timeouts}
 * sorted set (score = expiry epoch millis); this worker periodically
 * claims due rides and expires their offers. The claim lock keeps
 * multiple instances from double-processing the same timeout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RideTimeoutService {

    private final MatchingTimeoutRepository timeoutRepository;

    private final DriverOfferService offerService;

    private final ReassignmentService reassignmentService;

    @Scheduled(fixedDelayString = "${matching.timeout-scan-interval-ms:2000}")
    public void scanTimeouts() {
        long now = Instant.now().toEpochMilli();
        List<Long> due;
        try {
            due = timeoutRepository.dueTimeouts(now, 100);
        } catch (Exception ex) {
            log.warn("Timeout scan failed, will retry next tick", ex);
            return;
        }
        for (Long rideId : due) {
            if (timeoutRepository.claimTimeout(rideId)) {
                try {
                    OfferResolution resolution = offerService.expireOffer(rideId);
                    if (resolution == OfferResolution.EXPIRED) {
                        reassignmentService.reassign(rideId);
                    }
                } catch (Exception ex) {
                    log.error("Failed to process timeout for ride {}", rideId, ex);
                }
            }
        }
    }

}
