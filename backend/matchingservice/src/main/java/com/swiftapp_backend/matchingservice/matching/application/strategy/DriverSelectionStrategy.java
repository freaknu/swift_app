package com.swiftapp_backend.matchingservice.matching.application.strategy;

import com.swiftapp_backend.matchingservice.matching.domain.dto.MatchingContext;
import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStrategyType;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;

import java.util.List;

/**
 * Orders nearby candidates for lock attempts. The caller then walks the
 * ordered list and takes the first driver whose distributed lock
 * can be acquired, so selection and locking work together.
 */
public interface DriverSelectionStrategy {

    MatchingStrategyType type();

    List<DriverCandidate> order(List<DriverCandidate> candidates,
                                MatchingContext context);
}
