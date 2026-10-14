package com.swiftapp_backend.matchingservice.matching.application.strategy;

import com.swiftapp_backend.matchingservice.matching.domain.dto.MatchingContext;
import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStrategyType;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Spreads rides across nearby drivers instead of always hammering
 * the single nearest one. Rotates the candidate list by a Redis-backed
 * cursor, then the caller still takes the first lockable driver.
 */
@Component
@RequiredArgsConstructor
public class RoundRobinStrategy implements DriverSelectionStrategy {

    private final RoundRobinIndex roundRobinIndex;

    @Override
    public MatchingStrategyType type() {
        return MatchingStrategyType.ROUND_ROBIN;
    }

    @Override
    public List<DriverCandidate> order(List<DriverCandidate> candidates,
                                       MatchingContext context) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        int size = candidates.size();
        int start = (int) (Math.floorMod(roundRobinIndex.nextIndex(), size));
        List<DriverCandidate> rotated = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            rotated.add(candidates.get((start + i) % size));
        }
        return rotated;
    }
}
