package com.swiftapp_backend.matchingservice.matching.application.strategy;

import com.swiftapp_backend.matchingservice.matching.domain.dto.MatchingContext;
import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStrategyType;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Nearest driver first. The Redis GEO query already returns candidates
 * sorted ascending by distance; this re-sorts defensively so the
 * strategy holds even if the query ever changes.
 */
@Component
public class NearestDriverStrategy implements DriverSelectionStrategy {

    @Override
    public MatchingStrategyType type() {
        return MatchingStrategyType.NEAREST;
    }

    @Override
    public List<DriverCandidate> order(List<DriverCandidate> candidates,
                                       MatchingContext context) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble(DriverCandidate::getDistanceKm))
                .toList();
    }
}
