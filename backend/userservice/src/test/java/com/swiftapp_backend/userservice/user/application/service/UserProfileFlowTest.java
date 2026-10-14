package com.swiftapp_backend.userservice.user.application.service;

import com.swiftapp_backend.userservice.shared.ApiException;
import com.swiftapp_backend.userservice.user.api.dto.AddressCreateRequestDto;
import com.swiftapp_backend.userservice.user.api.dto.EmergencyContactDto;
import com.swiftapp_backend.userservice.user.api.dto.UserCreateRequestDto;
import com.swiftapp_backend.userservice.user.application.port.UserCache;
import com.swiftapp_backend.userservice.user.application.port.UserEventPublisherPort;
import com.swiftapp_backend.userservice.user.application.port.RideHistoryOwnershipPort;
import com.swiftapp_backend.userservice.user.domain.enums.AddressType;
import com.swiftapp_backend.userservice.user.domain.event.RideHistoryCreateEvent;
import com.swiftapp_backend.userservice.user.domain.event.UserCreateEvent;
import com.swiftapp_backend.userservice.user.domain.model.RideHistoryModel;
import com.swiftapp_backend.userservice.user.domain.repository.AddressRepository;
import com.swiftapp_backend.userservice.user.domain.repository.EmergencyContactRepository;
import com.swiftapp_backend.userservice.user.domain.repository.RideHistoryRepository;
import com.swiftapp_backend.userservice.user.domain.repository.UserModelRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserProfileFlowTest {
    @Autowired private UserService users;
    @Autowired private RideHistoryService history;
    @Autowired private UserModelRepository userRepository;
    @Autowired private RideHistoryRepository historyRepository;
    @Autowired private EmergencyContactRepository contacts;
    @Autowired private AddressRepository addresses;
    @MockitoBean private UserCache cache;
    @MockitoBean private UserEventPublisherPort publisher;
    @MockitoBean private RideHistoryOwnershipPort rideOwnership;

    @Test
    void signupReplayIsIdempotentAndBlankSocialPhonesDoNotCollide() {
        createRider(101L);
        createRider(101L);
        createRider(202L);

        assertEquals(2, userRepository.findAll().size());
        assertNull(userRepository.findByAuthUserId(101L).orElseThrow().getPhoneNumber());
        assertNull(userRepository.findByAuthUserId(202L).orElseThrow().getPhoneNumber());
        assertNotEquals(101L, userRepository.findByAuthUserId(101L).orElseThrow().getId());
    }

    @Test
    void emergencyContactsUseProfileIdAppendAndPreserveOwnership() throws Exception {
        createRider(303L);
        createRider(404L);
        Long profileId = userRepository.findByAuthUserId(303L).orElseThrow().getId();
        var first = contact("Friend", "Asha", "1111111111");
        var second = contact("Family", "Maya", "2222222222");

        Long firstId = users.saveEmergencyContact(List.of(first), 303L).getFirst().getId();
        var saved = users.saveEmergencyContact(List.of(second), 303L);
        assertEquals(2, saved.size());
        assertEquals(2, contacts.findAllByUser_Id(profileId).size());
        assertEquals(303L, contacts.findById(firstId).orElseThrow().getUser().getAuthUserId());

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class,
                () -> users.saveEmergencyContact(List.of(EmergencyContactDto.builder()
                        .id(firstId).contactType("Other").contactPersonName("Wrong")
                        .contactPersonNumber("9999999999").build()), 404L)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class,
                () -> users.deleteContact(404L, firstId)).getStatus());

        users.updateUser(303L, UserCreateRequestDto.builder().fullName("Rider 303")
                .email("rider303@example.invalid").phoneNumber("").build());
        assertEquals(2, contacts.findAllByUser_Id(profileId).size());
        assertEquals(1, users.deleteContact(303L, firstId).size());
        assertFalse(contacts.findById(firstId).orElseThrow().isActive());
    }

    @Test
    void addressDeleteChecksOwnerAndFavouriteIsPersisted() throws Exception {
        createRider(505L);
        createRider(606L);
        Long profileId = userRepository.findByAuthUserId(505L).orElseThrow().getId();
        var place = AddressCreateRequestDto.builder().addressType(AddressType.HOME)
                .label("Home").fullAddress("123 Example Road").city("Test City")
                .state("Test State").country("Test Country").pinCode("123456")
                .latitude(12.0).longitude(77.0).isFavouritePlace(true).build();
        Long addressId = users.addFavouritePlace(505L, place).getFirst().getId();

        assertEquals(1, addresses.findAllByUser_Id(profileId).size());
        assertTrue(Boolean.TRUE.equals(addresses.findById(addressId).orElseThrow().getIsFavouritePlace()));
        assertEquals(1, users.getFavouritePlaces(505L).size());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class,
                () -> users.deleteAddress(606L, addressId)).getStatus());
        users.deleteAddress(505L, addressId);
        assertTrue(users.getFavouritePlaces(505L).isEmpty());
    }

    @Test
    void rideHistoryProjectsRiderProfileToAuthIdentityAndDeduplicatesReplay() {
        createRider(707L);
        Long profileId = userRepository.findByAuthUserId(707L).orElseThrow().getId();
        var event = RideHistoryCreateEvent.builder().riderId(profileId).authUserId(profileId)
                .rideId(900L).pickUpLatitude(12.0).pickUpLongitude(77.0)
                .destinationLatitude(12.1).destinationLongitude(77.1).build();

        history.addRideHistory(event);
        history.addRideHistory(event);
        assertEquals(1, history.getRideHistory(707L).size());
        assertEquals(900L, history.getRideHistory(707L).getFirst().getRideId());
        assertEquals(profileId, historyRepository.findByAuthUserIdOrderByCreatedAtDesc(707L)
                .getFirst().getRiderProfileId());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class,
                () -> history.getRideHistory(profileId)).getStatus());
    }

    @Test
    void legacyHistoryIsRecoveredOnlyForActualRiderWhenProfileAndAuthIdsCollide() {
        createRider(1_000_007L);
        Long originalProfileId = userRepository.findByAuthUserId(1_000_007L).orElseThrow().getId();
        createRider(originalProfileId);
        Long collidingProfileId = userRepository.findByAuthUserId(originalProfileId).orElseThrow().getId();
        when(rideOwnership.riderProfileIdForRide(901L)).thenReturn(Optional.of(originalProfileId));

        historyRepository.save(RideHistoryModel.builder()
                .authUserId(originalProfileId).riderProfileId(collidingProfileId).rideId(902L).build());
        historyRepository.save(RideHistoryModel.builder()
                .authUserId(originalProfileId).rideId(901L).build());

        assertEquals(List.of(901L), history.getRideHistory(1_000_007L).stream()
                .map(ride -> ride.getRideId()).toList());
        assertEquals(List.of(902L), history.getRideHistory(originalProfileId, 0, 1).stream()
                .map(ride -> ride.getRideId()).toList());
    }

    @Test
    void historyFailsClosedWhenRideOwnershipIsUnavailable() {
        createRider(1_000_008L);
        Long profileId = userRepository.findByAuthUserId(1_000_008L).orElseThrow().getId();
        historyRepository.save(RideHistoryModel.builder()
                .authUserId(profileId).rideId(903L).build());
        when(rideOwnership.riderProfileIdForRide(903L)).thenThrow(new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE, "Ride history ownership could not be verified"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ApiException.class,
                () -> history.getRideHistory(1_000_008L)).getStatus());
    }

    private void createRider(Long authId) {
        users.createUserByKafka(UserCreateEvent.builder().authUserId(authId)
                .name("Rider " + authId).email("rider" + authId + "@example.invalid")
                .phoneNumber("").build());
    }

    private EmergencyContactDto contact(String type, String name, String number) {
        return EmergencyContactDto.builder().contactType(type)
                .contactPersonName(name).contactPersonNumber(number).build();
    }
}
