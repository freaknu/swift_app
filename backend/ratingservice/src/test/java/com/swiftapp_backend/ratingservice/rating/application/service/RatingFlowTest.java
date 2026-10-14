package com.swiftapp_backend.ratingservice.rating.application.service;

import com.swiftapp_backend.ratingservice.rating.api.dto.RateUserCreateRequestDto;
import com.swiftapp_backend.ratingservice.rating.application.port.RatingEventPublisher;
import com.swiftapp_backend.ratingservice.rating.application.port.RatingParticipantPort;
import com.swiftapp_backend.ratingservice.rating.domain.enums.UserType;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.repository.SpringDataRateUserRepository;
import com.swiftapp_backend.ratingservice.shared.ApiException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RatingFlowTest {
    @Autowired private RatingService ratings;
    @Autowired private RateUserService entries;
    @Autowired private SpringDataRateUserRepository entryRepository;
    @MockitoBean private RatingParticipantPort participants;
    @MockitoBean private RatingEventPublisher publisher;

    @Test
    void completedRideParticipantCanRateOnceAndOnlyAuthorCanDelete() {
        when(participants.ride(80L)).thenReturn(new RatingParticipantPort.Ride(
                80L, 31L, 42L, "COMPLETED"));
        when(participants.profileIdForAuthUser(101L, UserType.RIDER)).thenReturn(31L);
        when(participants.authUserIdForProfile(42L, UserType.DRIVER)).thenReturn(202L);
        var request = RateUserCreateRequestDto.builder().rideId(80L).driverId(42L)
                .userType(UserType.DRIVER).rate(4L).build();

        var created = ratings.addRating(101L, request, null);
        assertEquals(4.0, created.getAverageRating());
        assertEquals(1, created.getAllRatings().size());
        assertEquals(4.0, created.getAllRatings().getFirst().getRate());
        assertNull(created.getAllRatings().getFirst().getEmail());
        assertEquals(1, entryRepository.count());
        assertEquals(created.getId(), ratings.addRating(101L, request, null).getId());
        assertEquals(1, entryRepository.count());
        assertEquals(4.0, ratings.getMyRideRating(101L, 80L).getRate());
        assertNull(ratings.getMyRideRating(999L, 80L));
        ArgumentCaptor<Object> update = ArgumentCaptor.forClass(Object.class);
        verify(publisher, times(1)).publishRatingUpdated(update.capture());
        assertEquals(42L, ((Map<?, ?>) update.getValue()).get("driverId"));
        assertEquals(4.0, ((Map<?, ?>) update.getValue()).get("rating"));

        Long entryId = created.getAllRatings().getFirst().getId();
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> entries.deleteRating(999L, entryId)).getStatus());
        assertEquals(1, ratings.getRating(202L, 42L, null).getAllRatings().size());
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> ratings.getRating(999L, 42L, null)).getStatus());
        entries.deleteRating(101L, entryId);
        assertNull(ratings.getMyRideRating(101L, 80L));
        entries.deleteRating(101L, entryId);
        assertEquals(0.0, ratings.getRating(202L, 42L, null).getAverageRating());
        assertEquals(0, ratings.getRating(202L, 42L, null).getAllRatings().size());
        verify(publisher, times(2)).publishRatingUpdated(any());
    }

    @Test
    void rejectsPendingRideAndUnrelatedParticipant() {
        var request = RateUserCreateRequestDto.builder().rideId(81L).driverId(43L)
                .userType(UserType.DRIVER).rate(5L).build();
        when(participants.ride(81L)).thenReturn(new RatingParticipantPort.Ride(
                81L, 32L, 43L, "STARTED"));
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> ratings.addRating(102L, request, null)).getStatus());

        when(participants.ride(81L)).thenReturn(new RatingParticipantPort.Ride(
                81L, 32L, 43L, "COMPLETED"));
        when(participants.profileIdForAuthUser(102L, UserType.RIDER)).thenReturn(999L);
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> ratings.addRating(102L, request, null)).getStatus());
        assertEquals(0, entryRepository.count());
    }

    @Test
    void rejectsForgedRecipientAndOutOfRangeScore() {
        var request = RateUserCreateRequestDto.builder().rideId(82L).driverId(44L)
                .userType(UserType.DRIVER).rate(0L).build();
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ApiException.class, () -> ratings.addRating(103L, request, null)).getStatus());
        request.setRate(3L);
        when(participants.ride(82L)).thenReturn(new RatingParticipantPort.Ride(
                82L, 33L, 45L, "COMPLETED"));
        when(participants.profileIdForAuthUser(103L, UserType.RIDER)).thenReturn(33L);
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> ratings.addRating(103L, request, null)).getStatus());
        assertEquals(0, entryRepository.count());
    }
}
