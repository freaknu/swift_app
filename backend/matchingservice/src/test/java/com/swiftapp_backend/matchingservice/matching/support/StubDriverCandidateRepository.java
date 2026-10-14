package com.swiftapp_backend.matchingservice.matching.support;

import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverCandidateRepository;

import java.util.ArrayList;
import java.util.List;

/** Returns a fixed candidate list; records requested radii. */
public class StubDriverCandidateRepository implements DriverCandidateRepository {

    public final List<DriverCandidate> candidates = new ArrayList<>();

    public final List<Double> requestedRadii = new ArrayList<>();

    @Override
    public List<DriverCandidate> findCandidates(double latitude,
                                                double longitude,
                                                VehicleTypeEnum vehicleType,
                                                double radiusKm,
                                                long limit) {
        requestedRadii.add(radiusKm);
        return new ArrayList<>(candidates);
    }
}
