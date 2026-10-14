package com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.mapper;

import com.swiftapp_backend.ratingservice.rating.domain.enums.UserType;
import com.swiftapp_backend.ratingservice.rating.domain.model.RatingModel;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.entity.RateUserEntity;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.entity.RatingEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RatingPersistenceMapperTest {
    private final RatingPersistenceMapper mapper =
            new RatingPersistenceMapper(new RateUserPersistenceMapper());

    @Test
    void preservesRatingsAndRebuildsBackReferencesInBothDirections() {
        RatingEntity entity = RatingEntity.builder()
                .id(10L)
                .driverId(20L)
                .averageRating(4.5)
                .userType(UserType.DRIVER)
                .isActive(true)
                .build();
        RateUserEntity rate = RateUserEntity.builder()
                .id(30L)
                .userId(40L)
                .email("rater@example.com")
                .name("Rater")
                .rate(4.5)
                .isActive(true)
                .rating(entity)
                .build();
        entity.setAllRatings(new ArrayList<>(List.of(rate)));

        RatingModel domain = mapper.toDomain(entity);

        assertEquals(1, domain.getAllRatings().size());
        assertSame(domain, domain.getAllRatings().getFirst().getRating());

        RatingEntity roundTrip = mapper.toEntity(domain);

        assertEquals(1, roundTrip.getAllRatings().size());
        assertSame(roundTrip, roundTrip.getAllRatings().getFirst().getRating());
    }
}
