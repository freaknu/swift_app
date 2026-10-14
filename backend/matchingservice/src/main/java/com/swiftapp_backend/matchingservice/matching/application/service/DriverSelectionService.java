package com.swiftapp_backend.matchingservice.matching.application.service;

import com.swiftapp_backend.matchingservice.matching.application.config.MatchingProperties;
import com.swiftapp_backend.matchingservice.matching.application.strategy.DriverSelectionStrategy;
import com.swiftapp_backend.matchingservice.matching.domain.dto.MatchingContext;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Picks the configured ordering strategy for one matching attempt.
 */
@Service
@RequiredArgsConstructor
public class DriverSelectionService {

    private final List<DriverSelectionStrategy> strategies;

    private final MatchingProperties properties;

    public List<DriverCandidate> orderCandidates(List<DriverCandidate> candidates,
                                                MatchingContext context) {
        DriverSelectionStrategy strategy = strategies.stream()
                .filter(s -> s.type() == properties.getStrategy())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No DriverSelectionStrategy for " + properties.getStrategy()));
        return strategy.order(candidates, context);
    }
}
