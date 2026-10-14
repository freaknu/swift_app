package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository;

import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RecentLocationEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RideLocationQueryRepositoryImpl implements RideLocationQueryRepository {

    private final EntityManager entityManager;

    @Override
    public List<RecentLocationEntity> findPreviousDestinations(
            Long riderId,
            double latitude,
            double longitude,
            double radiusMeters
    ) {

        String sql = """
                SELECT
                    r.id,
                    r.destination_latitude,
                    r.destination_longitude,
                    r.requested_at
                FROM rides r
                WHERE
                    r.rider_id = :riderId

                    AND r.destination_latitude IS NOT NULL
                    AND r.destination_longitude IS NOT NULL

                    AND earth_distance(
                        ll_to_earth(
                            r.pickup_latitude,
                            r.pickup_longitude
                        ),
                        ll_to_earth(
                            :latitude,
                            :longitude
                        )
                    ) <= :radius

                ORDER BY r.requested_at DESC
                """;

        Query query = entityManager.createNativeQuery(sql);

        query.setParameter("riderId", riderId);
        query.setParameter("latitude", latitude);
        query.setParameter("longitude", longitude);
        query.setParameter("radius", radiusMeters);

        List<?> result = query.getResultList();

        List<RecentLocationEntity> locations =
                new ArrayList<>(result.size());

        for (Object row : result) {

            Object[] data = (Object[]) row;

            Long locationId =
                    data[0] != null
                            ? ((Number) data[0]).longValue()
                            : null;

            double destinationLatitude =
                    data[1] != null
                            ? ((Number) data[1]).doubleValue()
                            : 0.0;

            double destinationLongitude =
                    data[2] != null
                            ? ((Number) data[2]).doubleValue()
                            : 0.0;

            LocalDateTime rideDate = null;

            if (data[3] instanceof java.sql.Timestamp timestamp) {
                rideDate = timestamp.toLocalDateTime();
            } else if (data[3] instanceof LocalDateTime localDateTime) {
                rideDate = localDateTime;
            }

            RecentLocationEntity location = new RecentLocationEntity(
                    locationId,
                    destinationLatitude,
                    destinationLongitude,
                    "Previous destination",
                    rideDate);

            locations.add(location);
        }

        return locations;
    }
}
