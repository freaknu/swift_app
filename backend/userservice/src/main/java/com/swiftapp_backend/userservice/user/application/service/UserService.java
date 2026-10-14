package com.swiftapp_backend.userservice.user.application.service;

import com.swiftapp_backend.userservice.user.api.dto.*;
import com.swiftapp_backend.userservice.user.application.mapper.AddressMapper;
import com.swiftapp_backend.userservice.user.application.mapper.EmergencyContactMapper;
import com.swiftapp_backend.userservice.user.application.mapper.UserMapper;
import com.swiftapp_backend.userservice.user.application.mapper.UserPreferencesMapper;
import com.swiftapp_backend.userservice.user.application.port.UserCache;
import com.swiftapp_backend.userservice.user.application.port.UserEventPublisherPort;
import com.swiftapp_backend.userservice.user.domain.event.UserCreateEvent;
import com.swiftapp_backend.userservice.user.domain.event.UserUpdatedEvent;
import com.swiftapp_backend.userservice.user.domain.model.AddressModel;
import com.swiftapp_backend.userservice.user.domain.model.EmergencyContactModel;
import com.swiftapp_backend.userservice.user.domain.model.UserModel;
import com.swiftapp_backend.userservice.user.domain.model.UserPreferencesModel;
import com.swiftapp_backend.userservice.user.domain.repository.AddressRepository;
import com.swiftapp_backend.userservice.user.domain.repository.EmergencyContactRepository;
import com.swiftapp_backend.userservice.user.domain.repository.UserModelRepository;
import com.swiftapp_backend.userservice.user.domain.repository.UserPreferencesRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import com.swiftapp_backend.userservice.shared.ApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserModelRepository userModelRepository;
    private final AddressRepository addressRepository;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final EmergencyContactRepository emergencyContactRepository;
    private final EmergencyContactMapper emergencyContactMapper;
    private final OfferAppliedService offerAppliedService;
    private final UserCache userCache;
    private final UserEventPublisherPort userEventPublisher;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserPreferencesMapper userPreferencesMapper;

    public List<UserResponseDto> getAllUsers() {
        List<UserModel> allUsers = userModelRepository.findAll();
        return allUsers
                .stream()
                .map(userMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public void createUserByKafka(UserCreateEvent data) {
        if (data.getAuthUserId() == null) throw ApiException.badRequest("Missing auth identity");
        if (userModelRepository.findByAuthUserId(data.getAuthUserId()).isPresent()) return;
        UserModel user = UserModel.builder()
                .authUserId(data.getAuthUserId())
                .fullName(data.getName())
                .email(data.getEmail())
                .phoneNumber(normalizePhone(data.getPhoneNumber()))
                .build();
        user = userModelRepository.save(user);
        log.info("Created rider profile {}", user.getId());
    }
    @Transactional
    public UserResponseDto updateUser(Long authUserId, UserCreateRequestDto dto) {

        UserModel existingUser = requireUser(authUserId);
        if ((dto.getId() != null && !dto.getId().equals(existingUser.getId()))
                || (dto.getAuthUserId() != null && !dto.getAuthUserId().equals(authUserId))) {
            throw ApiException.forbidden("Authenticated user does not own this profile");
        }
        existingUser.setFullName(dto.getFullName());
        existingUser.setEmail(dto.getEmail());
        existingUser.setPhoneNumber(normalizePhone(dto.getPhoneNumber()));
        existingUser.setProfileImageUrl(dto.getProfileImageUrl());

        UserModel updatedUser = userModelRepository.save(existingUser);

        // Evict cache
        String key = "userData" + updatedUser.getAuthUserId();
        if (userCache.contains(key)) {
            userCache.evict(key);
        }

        // Publish user.updated
        UserUpdatedEvent event = UserUpdatedEvent.builder()
                .authUserId(updatedUser.getAuthUserId())
                .fullName(updatedUser.getFullName())
                .email(updatedUser.getEmail())
                .phoneNumber(updatedUser.getPhoneNumber())
                .profileImageUrl(updatedUser.getProfileImageUrl())
                .updatedAt(LocalDateTime.now())
                .build();
        userEventPublisher.publish("user.updated", "user" + updatedUser.getAuthUserId(), event);

        return userMapper.toResponseDto(updatedUser);
    }

    public UserResponseDto getUserDetail(Long userId) throws Exception {
        String key = "userData"+userId;

        // if user exists ->
        var cachedUser = userCache.find(key, UserResponseDto.class);
        if(cachedUser.isPresent()) {
            return cachedUser.get();
        }

        UserModel existingUser = requireUser(userId);

        List<Long> appliedOfferIds = offerAppliedService.appliedOfferId(userId);

        List<AddressModel> allAddress = existingUser.getAddresses();
        List<AddressCreateResponseDto> addressDto = allAddress
                .stream()
                .filter(a -> Boolean.TRUE.equals(a.getActive()))
                .map(addressMapper::toResponseDto).toList();
        UserResponseDto resUser = userMapper
                .toResponseDto(existingUser);
        resUser.setAllAddresses(addressDto);
        resUser.setAppliedOfferIds(appliedOfferIds);

        userCache.put(key, resUser, Duration.ofSeconds(10));
        return resUser;
    }

    @Transactional
    public UserResponseDto deactivateUser(Long userId) throws Exception {
        UserModel existingUser = userModelRepository.findByAuthUserId(userId)
                .orElseThrow(()-> new Exception("User Doesn't exists"));
        existingUser.setActive(false);
        existingUser = userModelRepository.save(existingUser);
        String key = "userData"+userId;
        if(userCache.contains(key)) {
            userCache.evict(key);
        }
        return userMapper.toResponseDto(existingUser);
    }

    @Transactional
    public List<AddressCreateResponseDto> addAddress(Long userId, AddressCreateRequestDto dto) throws Exception {
        UserModel existingUser = userModelRepository.findByAuthUserId(userId)
                .orElseThrow(()-> new Exception("User Doesn't exists"));

        log.info("User detail fetched {}",existingUser.getAuthUserId());

        List<AddressModel> existingAddress = existingUser.getAddresses();

        log.info("Address detail fetched {}",existingAddress);

        AddressModel address = addressMapper.toModel(dto);
        address.setUser(existingUser);
        address = addressRepository.save(address);
        existingAddress.add(address);
        existingUser.setAddresses(existingAddress);
        existingUser = userModelRepository.save(existingUser);

        List<AddressCreateResponseDto> allAddress = existingUser.getAddresses()
                .stream()
                .filter(AddressModel::getActive)
                .map(addressMapper::toResponseDto)
                .toList();
        // Reload the complete profile, including applied offers, on the next read.
        userCache.evict("userData" + userId);
        return allAddress;

    }

    @Transactional
    public void deleteAddress(Long userId,Long addressId) throws Exception {
        AddressModel existingAddress = addressRepository.findById(addressId)
                .orElseThrow(()->new Exception("Address Not found"));
        if (existingAddress.getUser() == null
                || !Objects.equals(userId, existingAddress.getUser().getAuthUserId())) {
            throw ApiException.forbidden("Address does not belong to the authenticated user");
        }
        existingAddress.setActive(false);
        addressRepository.save(existingAddress);
        userCache.evict("userData" + userId);
    }

    @Transactional
    public List<AddressCreateResponseDto> getFavouritePlaces(Long userId) throws Exception {
        UserModel existingUser = userModelRepository.findByAuthUserId(userId)
                .orElseThrow(() -> new Exception("User Doesn't exist"));
        return existingUser.getAddresses().stream()
                .filter(a -> Boolean.TRUE.equals(a.getActive()) && Boolean.TRUE.equals(a.getIsFavouritePlace()))
                .map(addressMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public List<AddressCreateResponseDto> addFavouritePlace(Long userId, AddressCreateRequestDto dto) throws Exception {
        dto.setIsFavouritePlace(true);
        return addAddress(userId, dto);
    }

    @Transactional
    public UserResponseDto updateProfilePhoto(Long userId, ProfilePhotoUpdateDto dto) throws Exception {
        UserModel existingUser = userModelRepository.findByAuthUserId(userId)
                .orElseThrow(() -> new Exception("User Doesn't exist"));
        existingUser.setProfileImageUrl(dto.getProfileImageUrl());
        UserModel saved = userModelRepository.save(existingUser);

        String key = "userData" + userId;
        if (userCache.contains(key)) {
            userCache.evict(key);
        }

        UserUpdatedEvent event = UserUpdatedEvent.builder()
                .authUserId(saved.getAuthUserId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .phoneNumber(saved.getPhoneNumber())
                .profileImageUrl(saved.getProfileImageUrl())
                .updatedAt(LocalDateTime.now())
                .build();
        userEventPublisher.publish("user.updated", "user" + saved.getAuthUserId(), event);

        return userMapper.toResponseDto(saved);
    }

    public UserPreferencesDto getPreferences(Long userId) {
        return userPreferencesRepository.findByUserId(userId)
                .map(userPreferencesMapper::toDto)
                .orElseGet(() -> UserPreferencesDto.builder()
                        .language("en")
                        .theme("SYSTEM")
                        .pushNotificationEnabled(true)
                        .emailNotificationEnabled(true)
                        .smsNotificationEnabled(true)
                        .preferredPaymentMethod("CASH")
                        .build());
    }

    @Transactional
    public UserPreferencesDto updatePreferences(Long userId, UserPreferencesDto dto) {
        UserPreferencesModel model = userPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> UserPreferencesModel.builder().userId(userId).build());
        model.setLanguage(dto.getLanguage());
        model.setTheme(dto.getTheme());
        model.setPushNotificationEnabled(dto.isPushNotificationEnabled());
        model.setEmailNotificationEnabled(dto.isEmailNotificationEnabled());
        model.setSmsNotificationEnabled(dto.isSmsNotificationEnabled());
        model.setPreferredPaymentMethod(dto.getPreferredPaymentMethod());
        model.setUpdatedAt(LocalDateTime.now());

        UserPreferencesModel saved = userPreferencesRepository.save(model);
        return userPreferencesMapper.toDto(saved);
    }

    @Transactional
    public List<EmergencyContactDto> saveEmergencyContact(List<EmergencyContactDto> datas,Long userId) throws Exception {
        UserModel existingUser = requireUser(userId);
        List<EmergencyContactModel> contacts = new ArrayList<>();
        for (EmergencyContactDto dto : datas) {
            EmergencyContactModel contact;
            if (dto.getId() != null) {
                contact = emergencyContactRepository.findById(dto.getId())
                        .orElseThrow(() -> ApiException.notFound("Contact not found"));
                requireContactOwner(contact, userId);
                contact.setContactType(dto.getContactType());
                contact.setContactPersonName(dto.getContactPersonName());
                contact.setContactPersonNumber(dto.getContactPersonNumber());
            } else {
                contact = emergencyContactMapper.toModel(dto);
                contact.setUser(existingUser);
            }
            contact.setActive(true);
            contacts.add(contact);
        }
        emergencyContactRepository.saveAll(contacts);
        return getAllContacts(userId);
    }

    public List<EmergencyContactDto> getAllContacts(Long userId) throws Exception {
        UserModel existingUser = requireUser(userId);
        return emergencyContactRepository.findAllByUser_Id(existingUser.getId())
                .stream()
                .filter(EmergencyContactModel::isActive)
                .map(emergencyContactMapper::toDto)
                .toList();
    }

    @Transactional
    public List<EmergencyContactDto> deleteContact(Long userId,Long contactId) throws Exception {
        UserModel existingUser = requireUser(userId);
        EmergencyContactModel existingContact = emergencyContactRepository.findById(contactId).
                orElseThrow(()-> new Exception("Contact not found"));
        requireContactOwner(existingContact, userId);
        existingContact.setActive(false);
        emergencyContactRepository.saveAll(List.of(existingContact));
        return emergencyContactRepository.findAllByUser_Id(existingUser.getId())
                .stream()
                .filter(EmergencyContactModel::isActive)
                .map(emergencyContactMapper::toDto)
                .toList();
    }

    private UserModel requireUser(Long authUserId) {
        UserModel user = userModelRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> ApiException.notFound("Rider profile is not ready"));
        if (!Boolean.TRUE.equals(user.getActive())) throw ApiException.notFound("Rider profile is inactive");
        return user;
    }

    private void requireContactOwner(EmergencyContactModel contact, Long authUserId) {
        if (contact.getUser() == null || !Objects.equals(authUserId, contact.getUser().getAuthUserId())) {
            throw ApiException.forbidden("Contact does not belong to the authenticated user");
        }
    }

    private String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }
}
