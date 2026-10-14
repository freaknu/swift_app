package com.swiftapp_backend.matchingservice.matching.application.strategy;

import com.swiftapp_backend.matchingservice.matching.domain.dto.MatchingContext;
import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStrategyType;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundRobinStrategyTest {

    @Test
    void rotatesThroughCandidates() {
        RoundRobinStrategy strategy = new RoundRobinStrategy(new FakeIndex());
        List<DriverCandidate> candidates = List.of(
                candidate(101L), candidate(102L), candidate(103L));

        assertEquals(List.of(101L, 102L, 103L), ids(strategy.order(candidates, context())));
        assertEquals(List.of(102L, 103L, 101L), ids(strategy.order(candidates, context())));
        assertEquals(List.of(103L, 101L, 102L), ids(strategy.order(candidates, context())));
        assertEquals(List.of(101L, 102L, 103L), ids(strategy.order(candidates, context())));
    }

    @Test
    void emptyStaysEmpty() {
        RoundRobinStrategy strategy = new RoundRobinStrategy(new FakeIndex());
        assertTrue(strategy.order(List.of(), context()).isEmpty());
    }

    @Test
    void typeIsRoundRobin() {
        assertEquals(MatchingStrategyType.ROUND_ROBIN,
                new RoundRobinStrategy(new FakeIndex()).type());
    }

    private static List<Long> ids(List<DriverCandidate> candidates) {
        return candidates.stream().map(DriverCandidate::getDriverId).toList();
    }

    private static MatchingContext context() {
        return MatchingContext.builder().rideId(1L).build();
    }

    private static DriverCandidate candidate(Long driverId) {
        return DriverCandidate.builder().driverId(driverId).build();
    }

    /** Deterministic stand-in for the Redis cursor. */
    static class FakeIndex implements RoundRobinIndex {
        private long next;

        @Override
        public long nextIndex() {
            return next++;
        }
    }
}
