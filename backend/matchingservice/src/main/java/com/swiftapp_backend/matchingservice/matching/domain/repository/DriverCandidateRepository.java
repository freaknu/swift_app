package com.swiftapp_backend.matchingservice.matching.domain.repository;

import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;

import java.util.List;

/**
 * Nearby-driver search returning candidates with their GEO distance.
 */
public interface DriverCandidateRepository {

    List<DriverCandidate> findCandidates(double latitude,
                                         double longitude,
                                         VehicleTypeEnum vehicleType,
                                         double radiusKm,
                                         long limit);
}
