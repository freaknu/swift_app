package com.swiftapp_backend.riderservice.ride.application.service;

import com.swiftapp_backend.riderservice.ride.domain.model.PairClass;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DistanceCalculateUtil {

    private static final double EARTH_RADIUS = 6371.0;
    private static final double RADIUS_METERS = 100.0;
    private static final double GRID_SIZE_METERS = 20.0;
    public Long distanceCoverDuration(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude,
            Long speed
    ) {

        if (speed == null || speed <= 0) {
            throw new IllegalArgumentException("Speed must be greater than 0");
        }

        double distance = calculateDistance(
                startLatitude,
                startLongitude,
                endLatitude,
                endLongitude
        );

        double timeInHours = distance / speed;
        double timeInSeconds = timeInHours * 3600;

        return Math.round(timeInSeconds);
    }

    public double calculateDistance(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {

        double latDistance = Math.toRadians(endLatitude - startLatitude);
        double lonDistance = Math.toRadians(endLongitude - startLongitude);

        double a =
                Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                        + Math.cos(Math.toRadians(startLatitude))
                        * Math.cos(Math.toRadians(endLatitude))
                        * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    public List<PairClass> getLatsLongs100Metres(
            double latitude,
            double longitude
    ) {

        List<PairClass> locations = new ArrayList<>();

        // Convert meters to latitude degrees
        double latitudeStep = GRID_SIZE_METERS / 111_320.0;

        // Convert meters to longitude degrees
        double longitudeStep =
                GRID_SIZE_METERS /
                        (111_320.0 * Math.cos(Math.toRadians(latitude)));

        // Generate points from -100m to +100m
        for (double x = -RADIUS_METERS;
             x <= RADIUS_METERS;
             x += GRID_SIZE_METERS) {

            for (double y = -RADIUS_METERS;
                 y <= RADIUS_METERS;
                 y += GRID_SIZE_METERS) {

                double newLatitude =
                        latitude + (x / 111_320.0);

                double newLongitude =
                        longitude +
                                (y /
                                        (111_320.0
                                                * Math.cos(Math.toRadians(latitude))));

                // Make sure generated point is actually
                // inside the 100 meter radius.
                double distance = calculateDistance(
                        latitude,
                        longitude,
                        newLatitude,
                        newLongitude
                ) * 1000; // KM -> meters

                if (distance <= RADIUS_METERS) {

                    locations.add(
                            PairClass.builder()
                                    .latitude(newLatitude)
                                    .longitude(newLongitude)
                                    .build()
                    );
                }
            }
        }

        return locations;
    }
}
