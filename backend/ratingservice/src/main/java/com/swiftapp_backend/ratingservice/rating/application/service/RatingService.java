package com.swiftapp_backend.ratingservice.rating.application.service;

import com.swiftapp_backend.ratingservice.rating.api.dto.RateUserCreateRequestDto;
import com.swiftapp_backend.ratingservice.rating.api.dto.RateUserCreateResponseDto;
import com.swiftapp_backend.ratingservice.rating.api.dto.RatingCreateRequestDto;
import com.swiftapp_backend.ratingservice.rating.api.dto.RatingCreateResponseDto;
import com.swiftapp_backend.ratingservice.rating.application.mapper.RateUserMapper;
import com.swiftapp_backend.ratingservice.rating.application.mapper.RatingMapper;
import com.swiftapp_backend.ratingservice.rating.application.port.RatingEventPublisher;
import com.swiftapp_backend.ratingservice.rating.application.port.RatingParticipantPort;
import com.swiftapp_backend.ratingservice.rating.domain.enums.UserType;
import com.swiftapp_backend.ratingservice.rating.domain.model.RateUserModel;
import com.swiftapp_backend.ratingservice.rating.domain.model.RatingModel;
import com.swiftapp_backend.ratingservice.rating.domain.repository.RateUserRepository;
import com.swiftapp_backend.ratingservice.rating.domain.repository.RatingRepository;
import com.swiftapp_backend.ratingservice.shared.ApiException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final RateUserMapper rateUserMapper;
    private final RatingMapper ratingMapper;
    private final RateUserService rateUserService;
    private final RateUserRepository rateUserRepository;
    private final RatingEventPublisher ratingEventPublisher;
    private final RatingParticipantPort participants;

    /** The recipient may view the aggregate and its anonymous entries. */
    public RatingCreateResponseDto getRating(Long authUserId, Long driverId, Long riderId) {
        UserType recipientRole = recipientRole(driverId, riderId);
        Long recipientId = recipientRole == UserType.DRIVER ? driverId : riderId;
        if (authUserId == null || !authUserId.equals(
                participants.authUserIdForProfile(recipientId, recipientRole))) {
            throw ApiException.forbidden("Only the rating recipient may view individual ratings");
        }
        RatingModel rating = findRecipientRating(recipientRole, recipientId)
                .orElseThrow(() -> ApiException.notFound("Rating was not found"));
        return toResponse(rating);
    }

    /** Returns only this authenticated author's active rating for a ride. */
    public RateUserCreateResponseDto getMyRideRating(Long authUserId, Long rideId) {
        if (authUserId == null || authUserId <= 0 || rideId == null || rideId <= 0) {
            throw ApiException.badRequest("Authenticated user and ride ID are required");
        }
        return rateUserRepository.findByRideIdAndRaterAuthUserId(rideId, authUserId)
                .filter(rate -> Boolean.TRUE.equals(rate.getIsActive()))
                .map(rateUserMapper::toDto).orElse(null);
    }

    @Transactional
    public RatingCreateResponseDto addRating(Long authUserId, RateUserCreateRequestDto data,
                                               Long requestedRatingId) {
        if (authUserId == null || authUserId <= 0 || data == null || data.getRideId() == null
                || data.getRideId() <= 0 || data.getRate() == null
                || data.getRate() < 1 || data.getRate() > 5) {
            throw ApiException.badRequest("Ride ID and a score between 1 and 5 are required");
        }
        UserType recipientRole = recipientRole(data.getDriverId(), data.getRiderId());
        if (data.getUserType() != recipientRole) {
            throw ApiException.badRequest("userType must identify the rating recipient");
        }
        RatingParticipantPort.Ride ride = participants.ride(data.getRideId());
        if (!Objects.equals(data.getRideId(), ride.id())
                || !"COMPLETED".equals(ride.currentStatus())) {
            throw ApiException.forbidden("Only a completed ride may be rated");
        }
        Long recipientId = recipientRole == UserType.DRIVER ? data.getDriverId() : data.getRiderId();
        Long rideRecipientId = recipientRole == UserType.DRIVER ? ride.driverId() : ride.riderId();
        Long rideRaterId = recipientRole == UserType.DRIVER ? ride.riderId() : ride.driverId();
        UserType raterRole = recipientRole == UserType.DRIVER ? UserType.RIDER : UserType.DRIVER;
        if (rideRecipientId == null || !rideRecipientId.equals(recipientId)
                || rideRaterId == null || !rideRaterId.equals(
                    participants.profileIdForAuthUser(authUserId, raterRole))) {
            throw ApiException.forbidden("Authenticated user did not participate in this ride");
        }

        RatingModel rating = findRecipientRating(recipientRole, recipientId).orElse(null);
        if (requestedRatingId != null && (rating == null
                || !requestedRatingId.equals(rating.getId()))) {
            throw ApiException.badRequest("ratingId does not identify this recipient");
        }
        RateUserModel prior = rateUserRepository.findByRideIdAndRaterAuthUserId(
                data.getRideId(), authUserId).orElse(null);
        if (prior != null) {
            if (Boolean.TRUE.equals(prior.getIsActive())
                    && Double.compare(prior.getRate(), data.getRate()) == 0
                    && rating != null && prior.getRating() != null
                    && Objects.equals(prior.getRating().getId(), rating.getId())) {
                return toResponse(rating);
            }
            throw ApiException.conflict("This ride has already been rated by this participant");
        }
        if (rating == null) {
            rating = ratingRepository.save(ratingMapper.toModel(RatingCreateRequestDto.builder()
                    .driverId(recipientRole == UserType.DRIVER ? recipientId : null)
                    .riderId(recipientRole == UserType.RIDER ? recipientId : null)
                    .userType(recipientRole).build()));
        }

        RateUserModel rate = RateUserModel.builder()
                .userId(rideRaterId)
                .raterAuthUserId(authUserId)
                .rideId(data.getRideId())
                .rate(data.getRate())
                .rating(rating)
                .build();
        rate = rateUserRepository.save(rate);
        if (rating.getAllRatings() == null) rating.setAllRatings(new ArrayList<>());
        rating.getAllRatings().add(rate);
        rating.setAverageRating(rating.getAllRatings().stream()
                .filter(candidate -> Boolean.TRUE.equals(candidate.getIsActive()))
                .mapToDouble(RateUserModel::getRate).average().orElse(0));
        rating = ratingRepository.save(rating);
        ratingEventPublisher.publishRatingUpdated(event(rating));
        return toResponse(rating);
    }

    private RatingCreateResponseDto toResponse(RatingModel rating) {
        List<RateUserCreateResponseDto> entries = rateUserService.getAllRateUsers(rating.getId())
                .stream().map(rateUserMapper::toDto).toList();
        RatingCreateResponseDto result = ratingMapper.toDto(rating);
        result.setAllRatings(entries);
        return result;
    }

    private Optional<RatingModel> findRecipientRating(UserType role, Long id) {
        return role == UserType.DRIVER
                ? ratingRepository.findByDriverId(id) : ratingRepository.findByRiderId(id);
    }

    private static UserType recipientRole(Long driverId, Long riderId) {
        if ((driverId == null) == (riderId == null)
                || (driverId != null && driverId <= 0)
                || (riderId != null && riderId <= 0)) {
            throw ApiException.badRequest("Exactly one positive recipient ID is required");
        }
        return driverId == null ? UserType.RIDER : UserType.DRIVER;
    }

    private static Map<String, Object> event(RatingModel rating) {
        Map<String, Object> event = new HashMap<>();
        event.put("ratingId", rating.getId());
        event.put("averageRating", rating.getAverageRating());
        // Driver statistics currently consumes the `rating` key.
        event.put("rating", rating.getAverageRating());
        if (rating.getDriverId() != null) event.put("driverId", rating.getDriverId());
        if (rating.getRiderId() != null) event.put("riderId", rating.getRiderId());
        return event;
    }
}
