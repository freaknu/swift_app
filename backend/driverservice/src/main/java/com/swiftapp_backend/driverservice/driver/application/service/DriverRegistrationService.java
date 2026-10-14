package com.swiftapp_backend.driverservice.driver.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.api.dto.*;
import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.application.mapper.DriverDetailMapper;
import com.swiftapp_backend.driverservice.driver.application.mapper.DriverDocumentMapper;
import com.swiftapp_backend.driverservice.driver.application.mapper.DriverEarningMapper;
import com.swiftapp_backend.driverservice.driver.application.mapper.VehicleMapper;
import com.swiftapp_backend.driverservice.driver.application.port.DriverCachePort;
import com.swiftapp_backend.driverservice.driver.application.port.DriverEventPublisherPort;
import com.swiftapp_backend.driverservice.driver.application.port.DriverNotificationSettingsPort;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverAvailability;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverStatus;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverDocumentModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverEarningSummaryModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverNotificationSettings;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverDocumentRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverEarningRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverRegistrationService {

    private final DriverRepository driverRepository;
    private final DriverDocumentRepository driverDocumentRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverEarningRepository driverEarningRepository;
    private final DriverEarningMapper driverEarningMapper;
    private final DriverEventPublisherPort eventPublisher;

    private final DriverCachePort driverCache;
    private final ObjectMapper objectMapper;

    private final DriverCacheService driverCacheService;
    private final DriverNotificationSettingsPort notificationSettings;

    /** Idempotent account seed from the auth service's driver-created event. */
    @Transactional
    public void createDriverAccount(Long authUserId, String name, String email, String phoneNumber) {
        if (authUserId == null) {
            throw new IllegalArgumentException("driver-created event missing authUserId");
        }
        if (driverRepository.findByAuthUserId(authUserId).isPresent()) {
            log.info("driver already exists for authUserId {}", authUserId);
            return;
        }
        DriverModel driver = DriverModel.builder()
                .authUserId(authUserId)
                .driverName(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .driverStatus(DriverStatus.OFFLINE)
                .isActive(true)
                .isAcceptingRides(false)
                .isVerified(false)
                .build();
        DriverModel saved = driverRepository.save(driver);
        log.info("driver detail saved: id={}, authUserId={}", saved.getId(), authUserId);
    }

    @Transactional
    public DriverResponseDto registerDriver(Long authUserId,
                                            DriverCreateRequestDto data) {

        boolean isNew = false;
        Optional<DriverModel> opt = driverRepository.findByAuthUserId(authUserId);
        DriverModel driver;
        if (opt.isEmpty()) {
            isNew = true;
            driver = new DriverModel();
            driver.setAuthUserId(authUserId);
            driver.setActive(true);
            driver.setDriverStatus(DriverStatus.OFFLINE);
            driver.setAcceptingRides(false);
        } else {
            driver = opt.get();
        }

        // Driver Name
        if (data.getDriverName() != null && !data.getDriverName().isBlank()) {
            driver.setDriverName(data.getDriverName());
        }

        // Email
        if (data.getEmail() != null && !data.getEmail().isBlank()) {
            driver.setEmail(data.getEmail());
        }

        // Phone Number
        if (data.getPhoneNumber() != null && !data.getPhoneNumber().isBlank()) {
            driver.setPhoneNumber(data.getPhoneNumber());
        }

        // Gender
        if (data.getGender() != null) {
            driver.setGender(data.getGender());
        }

        // Profile Image
        if (data.getProfileImage() != null && !data.getProfileImage().isBlank()) {
            driver.setProfileImage(data.getProfileImage());
        }

        // Availability is managed only by the status and matching flows.
        // Registration payloads may contain old client fields; ignore them.

        // Date Of Birth
        if (data.getDateOfBirth() != null) {
            driver.setDateOfBirth(data.getDateOfBirth());
        }

        // Languages
        if (data.getLanguages() != null) {
            driver.setLanguages(data.getLanguages());
        }

        // Driver Preferences
        if (data.getDriverPreferences() != null) {
            driver.setDriverPreferences(data.getDriverPreferences());
        }

        // Save Driver First
        DriverModel savedDriver = driverRepository.save(driver);

        driverCacheService.cacheDriverSummary(savedDriver);

        List<DriverDocumentModel> savedDocuments = new ArrayList<>();
        // Save Documents
        if (data.getAllDocuments() != null && !data.getAllDocuments().isEmpty()) {

            List<DriverDocumentModel> documents = data.getAllDocuments()
                    .stream()
                    .map(DriverDocumentMapper::toModel)
                    .peek(document -> {
                        document.setDriver(savedDriver);
                        document.setAuthUserId(authUserId);
                        document.setActive(true);
                        document.setUploadedAt(LocalDateTime.now());
                    })
                    .toList();

             savedDocuments =
                    driverDocumentRepository.saveAll(documents);

            savedDriver.setAllDocuments(savedDocuments);
        }

        List<VehicleModel> savedVehicles = new ArrayList<>();
        // Save Vehicles
        if (data.getAllVehicles() != null && !data.getAllVehicles().isEmpty()) {

            List<VehicleModel> vehicles = data.getAllVehicles()
                    .stream()
                    .map(VehicleMapper::toModel)
                    .peek(vehicle -> {
                        vehicle.setDriver(savedDriver);
                        vehicle.setAuthUserId(authUserId);
                        vehicle.setActive(true);
                    })
                    .toList();

             savedVehicles =
                    vehicleRepository.saveAll(vehicles);

            savedDriver.setAllVehicles(savedVehicles);
        }

        driverCache.delete("driver" + authUserId);
        driverCache.delete("driverVehicles" + authUserId);

        DriverResponseDto responseDto = DriverDetailMapper.toResponseDto(savedDriver);

        if (isNew) {
            eventPublisher.publish("driver.created", "driver" + authUserId, responseDto);
        } else {
            eventPublisher.publish("driver.updated", "driver" + authUserId, responseDto);
        }

        return responseDto;
    }


    @Transactional(readOnly = true)
    public ProfileCompletenessDto getProfileCompleteness(Long authUserId) throws Exception {
        DriverModel driver = driverRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new Exception("User Not found"));

        List<DriverDocumentModel> documents =
                driverDocumentRepository.findAllByAuthUserId(authUserId);
        List<VehicleModel> vehicles =
                vehicleRepository.findAllByAuthUserId(authUserId);

        // ---- personal details ----
        List<String> missingPersonal = new java.util.ArrayList<>();
        if (isBlank(driver.getDriverName())) missingPersonal.add("driverName");
        if (isBlank(driver.getEmail())) missingPersonal.add("email");
        if (isBlank(driver.getPhoneNumber())) missingPersonal.add("phoneNumber");
        if (driver.getGender() == null) missingPersonal.add("gender");
        if (driver.getDateOfBirth() == null) missingPersonal.add("dateOfBirth");

        // ---- documents: every DocumentType must have an active upload ----
        java.util.Set<DocumentType> uploaded = documents.stream()
                .filter(DriverDocumentModel::isActive)
                .map(DriverDocumentModel::getDocumentType)
                .collect(java.util.stream.Collectors.toSet());
        List<String> missingDocuments = java.util.Arrays.stream(DocumentType.values())
                .filter(t -> !uploaded.contains(t))
                .map(DocumentType::name)
                .collect(java.util.stream.Collectors.toList());

        // ---- vehicle: at least one active vehicle with type + key fields ----
        List<String> missingVehicle = new java.util.ArrayList<>();
        boolean hasVehicle = vehicles.stream()
                .filter(VehicleModel::isActive)
                .anyMatch(v -> v.getVehicleType() != null
                        && !isBlank(v.getBrand())
                        && !isBlank(v.getModel())
                        && !isBlank(v.getRegistrationNumber()));
        if (!hasVehicle) {
            missingVehicle.add("vehicle");
        }

        boolean complete = missingPersonal.isEmpty()
                && missingDocuments.isEmpty()
                && missingVehicle.isEmpty();

        List<String> allMissing = new java.util.ArrayList<>();
        allMissing.addAll(missingPersonal);
        allMissing.addAll(missingDocuments);
        allMissing.addAll(missingVehicle);

        return ProfileCompletenessDto.builder()
                .complete(complete)
                .personal(ProfileCompletenessDto.SectionStatus.builder()
                        .complete(missingPersonal.isEmpty())
                        .missing(missingPersonal)
                        .build())
                .documents(ProfileCompletenessDto.SectionStatus.builder()
                        .complete(missingDocuments.isEmpty())
                        .missing(missingDocuments)
                        .build())
                .vehicle(ProfileCompletenessDto.SectionStatus.builder()
                        .complete(missingVehicle.isEmpty())
                        .missing(missingVehicle)
                        .build())
                .missing(allMissing)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Transactional(readOnly = true)
    public DriverResponseDto getDriverProfile(Long authUserId)throws Exception {
        try {
            String key = "driver"+authUserId;
            if(driverCache.exists(key)) {
                try {
                    Object cached = driverCache.get(key);
                    if (cached != null) {
                        DriverResponseDto profile = objectMapper.convertValue(cached,
                                DriverResponseDto.class);
                        if (profile != null) return profile;
                    }
                } catch (RuntimeException ex) {
                    log.warn("Discarding unreadable driver profile cache for user {}", authUserId, ex);
                    driverCache.delete(key);
                }
            }
            Optional<DriverModel> existingDriver = driverRepository.findByAuthUserId(authUserId);

            if(existingDriver.isEmpty()) throw new Exception("User Not found");
            log.info("the driver profile is {}",existingDriver.get().getAuthUserId());
            existingDriver.get().setAllVehicles(
                    vehicleRepository.findAllByAuthUserId(authUserId).stream()
                            .filter(VehicleModel::isActive)
                            .toList()
            );
            existingDriver.get().setAllDocuments(
                    driverDocumentRepository.findAllByAuthUserId(authUserId).stream()
                            .filter(DriverDocumentModel::isActive)
                            .toList()
            );
            log.info("the response is {}",existingDriver.get().getDateOfBirth());
            DriverResponseDto res = DriverDetailMapper.toResponseDto(existingDriver.get());
            driverCache.set(key,res,20L, TimeUnit.SECONDS);
            log.info("the response is {}",res);
            return res;
        }
        catch (Exception e) {
            log.error("Error while mapping DriverResponseDto", e);
            throw e;
        }
    }

    @Transactional
    public void changeStatus(Long authUserId, DriverStatus status) throws Exception {
        Optional<DriverModel> existingDriver = driverRepository.findByAuthUserId(authUserId);
        if(existingDriver.isEmpty()) throw new Exception("User Not found");
        if (status == DriverStatus.ONLINE) {
            if (!existingDriver.get().isActive()
                    || existingDriver.get().getDriverStatus() == DriverStatus.SUSPENDED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver account is suspended");
            }
            if (!getProfileCompleteness(authUserId).isComplete()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Complete your driver profile, documents, and vehicle before going online");
            }
        }
        driverCacheService.cacheStatusChange(authUserId,status);
        existingDriver.get().setDriverStatus(status);
        driverRepository.save(existingDriver.get());
        driverCache.delete("driver" + authUserId);
        eventPublisher.publish("driver.matching.status", String.valueOf(existingDriver.get().getId()),
                java.util.Map.of("driverId", existingDriver.get().getId(),
                        "status", status == DriverStatus.ONLINE ? "ACTIVE" : "INACTIVE"));

        if (status == DriverStatus.ONLINE) {
            eventPublisher.publish("driver.online", "driver" + authUserId, "Driver is now ONLINE");
        } else if (status == DriverStatus.OFFLINE) {
            eventPublisher.publish("driver.offline", "driver" + authUserId, "Driver is now OFFLINE");
        }
    }

    @Transactional
    public void suspendDriver(Long authUserId, String reason) throws Exception {
        DriverModel driver = driverRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new Exception("Driver not found"));
        boolean wasActive = driver.isActive();
        driver.setActive(false);
        driver.setDriverStatus(DriverStatus.OFFLINE);
        driver.setDriverAvailability(DriverAvailability.OFFLINE);
        driverRepository.save(driver);

        String key = "driver" + authUserId;
        driverCache.delete(key);
        eventPublisher.publish("driver.unavailable", "driver" + authUserId, "Suspended: " + reason);
        if (wasActive) {
            eventPublisher.publish("driver.account.status.changed", "driver" + authUserId,
                    Map.of("eventId", UUID.randomUUID().toString(),
                            "driverId", driver.getId(),
                            "authUserId", authUserId,
                            "status", "SUSPENDED",
                            "eventTime", Instant.now().toString(),
                            "reason", reason == null || reason.isBlank()
                                    ? "No reason provided." : reason.trim()));
        }
    }

    @Transactional
    public void reactivateDriver(Long authUserId) throws Exception {
        DriverModel driver = driverRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new Exception("Driver not found"));
        boolean wasActive = driver.isActive();
        driver.setActive(true);
        driverRepository.save(driver);

        String key = "driver" + authUserId;
        driverCache.delete(key);
        eventPublisher.publish("driver.available", "driver" + authUserId, "Driver reactivated");
        if (!wasActive) {
            eventPublisher.publish("driver.account.status.changed", "driver" + authUserId,
                    Map.of("eventId", UUID.randomUUID().toString(),
                            "driverId", driver.getId(),
                            "authUserId", authUserId,
                            "status", "REACTIVATED",
                            "eventTime", Instant.now().toString(),
                            "reason", "Account reactivated"));
        }
    }

    @Transactional(readOnly = true)
    public DriverEarningSummaryDto getEarnings(Long authUserId) {
        DriverModel driver = driverRepository.findByAuthUserId(authUserId).orElse(null);
        if (driver == null) {
            return DriverEarningSummaryDto.builder()
                    .thisMonthIncome(0L)
                    .thisWeekIncome(0L)
                    .thisLifetimeIncome(0L)
                    .build();
        }
        return driverEarningRepository.findByDriverId(driver.getId())
                .map(driverEarningMapper::toDto)
                .orElseGet(() -> DriverEarningSummaryDto.builder()
                        .thisMonthIncome(0L)
                        .thisWeekIncome(0L)
                        .thisLifetimeIncome(0L)
                        .build());
    }

    @Transactional
    public DriverNotificationPreferencesDto getNotificationPreferences(Long authUserId) {
        DriverModel driver = notificationDriver(authUserId);
        Optional<DriverNotificationSettings> persisted =
                notificationSettings.find(driver.getId(), authUserId);
        if (persisted.isPresent()) return toNotificationDto(persisted.get());

        String key = "driverNotifPref:" + authUserId;
        DriverNotificationPreferencesDto cached = null;
        try {
            if (driverCache.exists(key)) {
                cached = objectMapper.convertValue(
                        driverCache.get(key), DriverNotificationPreferencesDto.class);
            }
        } catch (RuntimeException ex) {
            log.warn("Unreadable legacy notification preferences for driver {}", driver.getId());
            evictNotificationCache(key);
        }
        if (cached != null) {
            DriverNotificationPreferencesDto migrated = toNotificationDto(
                    notificationSettings.save(toNotificationSettings(driver, authUserId, cached)));
            evictNotificationCache(key);
            return migrated;
        }
        return DriverNotificationPreferencesDto.builder()
                .pushEnabled(true)
                .smsEnabled(true)
                .emailEnabled(true)
                .rideAlertsEnabled(true)
                .surgeAlertsEnabled(true)
                .build();
    }

    @Transactional
    public DriverNotificationPreferencesDto updateNotificationPreferences(Long authUserId, DriverNotificationPreferencesDto dto) {
        if (dto == null) throw new IllegalArgumentException("Notification preferences are required");
        DriverModel driver = notificationDriver(authUserId);
        DriverNotificationSettings saved = notificationSettings.save(
                toNotificationSettings(driver, authUserId, dto));
        String key = "driverNotifPref:" + authUserId;
        evictNotificationCache(key);
        return toNotificationDto(saved);
    }

    private DriverModel notificationDriver(Long authUserId) {
        if (authUserId == null || authUserId <= 0) {
            throw new IllegalArgumentException("Authenticated driver is required");
        }
        return driverRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new IllegalArgumentException("Driver profile not found"));
    }

    private static DriverNotificationSettings toNotificationSettings(
            DriverModel driver, Long authUserId, DriverNotificationPreferencesDto dto) {
        return new DriverNotificationSettings(driver.getId(), authUserId,
                dto.isPushEnabled(), dto.isSmsEnabled(), dto.isEmailEnabled(),
                dto.isRideAlertsEnabled(), dto.isSurgeAlertsEnabled());
    }

    private static DriverNotificationPreferencesDto toNotificationDto(DriverNotificationSettings settings) {
        return DriverNotificationPreferencesDto.builder()
                .pushEnabled(settings.pushEnabled())
                .smsEnabled(settings.smsEnabled())
                .emailEnabled(settings.emailEnabled())
                .rideAlertsEnabled(settings.rideAlertsEnabled())
                .surgeAlertsEnabled(settings.surgeAlertsEnabled())
                .build();
    }

    private void evictNotificationCache(String key) {
        try {
            driverCache.delete(key);
        } catch (RuntimeException ex) {
            log.warn("Could not evict legacy driver notification cache");
        }
    }

    public void saveDeviceInfo(Long authUserId, DriverDeviceInfoRequestDto dto) {
        String key = "driverDevice:" + authUserId;
        driverCache.set(key, dto, 30L, TimeUnit.DAYS);
    }
}
