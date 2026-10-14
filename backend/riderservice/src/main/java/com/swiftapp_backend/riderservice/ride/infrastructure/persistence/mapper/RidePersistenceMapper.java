package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.mapper;

import com.swiftapp_backend.riderservice.ride.domain.model.FareDetailModel;
import com.swiftapp_backend.riderservice.ride.domain.model.OfferModel;
import com.swiftapp_backend.riderservice.ride.domain.model.RecentLocation;
import com.swiftapp_backend.riderservice.ride.domain.model.RideDriverModel;
import com.swiftapp_backend.riderservice.ride.domain.model.RideModel;
import com.swiftapp_backend.riderservice.ride.domain.model.RideStatusHistoryModel;
import com.swiftapp_backend.riderservice.ride.domain.model.RideTypesModel;
import com.swiftapp_backend.riderservice.ride.domain.model.RideUserModel;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.FareDetailEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.OfferEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RecentLocationEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideDriverEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideStatusHistoryEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideTypesEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideUserEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates between framework-neutral ride domain models and persistence entities.
 * Identity based mapping contexts keep the aggregate graph cycle-safe and retain
 * the bidirectional Ride/Fare and Ride/History relationships.
 */
@Component
public class RidePersistenceMapper {

    public RecentLocation toDomain(RecentLocationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RecentLocation(
                entity.locationId(),
                entity.destinationLatitude(),
                entity.destinationLongitude(),
                entity.message(),
                entity.rideDate());
    }

    public RideEntity toEntity(RideModel model) {
        return toRideEntity(model, new IdentityHashMap<>());
    }

    public RideModel toDomain(RideEntity entity) {
        return toRideModel(entity, new IdentityHashMap<>());
    }

    public FareDetailEntity toEntity(FareDetailModel model) {
        return toFareDetailEntity(model, new IdentityHashMap<>());
    }

    public FareDetailModel toDomain(FareDetailEntity entity) {
        return toFareDetailModel(entity, new IdentityHashMap<>());
    }

    public OfferEntity toEntity(OfferModel model) {
        return toOfferEntity(model, new IdentityHashMap<>());
    }

    public OfferModel toDomain(OfferEntity entity) {
        return toOfferModel(entity, new IdentityHashMap<>());
    }

    public RideDriverEntity toEntity(RideDriverModel model) {
        return toRideDriverEntity(model, new IdentityHashMap<>());
    }

    public RideDriverModel toDomain(RideDriverEntity entity) {
        return toRideDriverModel(entity, new IdentityHashMap<>());
    }

    public RideStatusHistoryEntity toEntity(RideStatusHistoryModel model) {
        return toRideStatusHistoryEntity(model, new IdentityHashMap<>());
    }

    public RideStatusHistoryModel toDomain(RideStatusHistoryEntity entity) {
        return toRideStatusHistoryModel(entity, new IdentityHashMap<>());
    }

    public List<RideStatusHistoryModel> toHistoryDomain(List<RideStatusHistoryEntity> entities) {
        if (entities == null) {
            return null;
        }
        Map<Object, Object> context = new IdentityHashMap<>();
        List<RideStatusHistoryModel> models = new ArrayList<>(entities.size());
        for (RideStatusHistoryEntity entity : entities) {
            models.add(toRideStatusHistoryModel(entity, context));
        }
        return models;
    }

    public RideTypesEntity toEntity(RideTypesModel model) {
        return toRideTypesEntity(model, new IdentityHashMap<>());
    }

    public RideTypesModel toDomain(RideTypesEntity entity) {
        return toRideTypesModel(entity, new IdentityHashMap<>());
    }

    public RideUserEntity toEntity(RideUserModel model) {
        return toRideUserEntity(model, new IdentityHashMap<>());
    }

    public RideUserModel toDomain(RideUserEntity entity) {
        return toRideUserModel(entity, new IdentityHashMap<>());
    }

    private RideEntity toRideEntity(RideModel model, Map<Object, Object> context) {
        if (model == null) {
            return null;
        }
        RideEntity mapped = (RideEntity) context.get(model);
        if (mapped != null) {
            return mapped;
        }

        RideEntity entity = RideEntity.builder()
                .id(model.getId())
                .version(model.getVersion())
                .riderId(model.getRiderId())
                .activeRiderId(model.getActiveRiderId())
                .driverId(model.getDriverId())
                .activeDriverId(model.getActiveDriverId())
                .vehicleId(model.getVehicleId())
                .rideTypeCode(model.getRideTypeCode())
                .currentStatus(model.getCurrentStatus())
                .rideType(model.getRideType())
                .pickupLatitude(model.getPickupLatitude())
                .pickupLongitude(model.getPickupLongitude())
                .destinationLatitude(model.getDestinationLatitude())
                .destinationLongitude(model.getDestinationLongitude())
                .estimatedDistance(model.getEstimatedDistance())
                .actualDistance(model.getActualDistance())
                .isCompleted(model.getIsCompleted())
                .estimatedDuration(model.getEstimatedDuration())
                .actualDuration(model.getActualDuration())
                .requestedAt(model.getRequestedAt())
                .acceptedAt(model.getAcceptedAt())
                .driverArrivedAt(model.getDriverArrivedAt())
                .startedAt(model.getStartedAt())
                .isRideCancelled(model.getIsRideCancelled())
                .completedAt(model.getCompletedAt())
                .cancelledAt(model.getCancelledAt())
                .cancelledBy(model.getCancelledBy())
                .cancellationReason(model.getCancellationReason())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
        context.put(model, entity);

        entity.setOffer(toOfferEntity(model.getOffer(), context));

        FareDetailEntity fareDetail = toFareDetailEntity(model.getFareDetail(), context);
        entity.setFareDetail(fareDetail);
        if (fareDetail != null) {
            fareDetail.setRide(entity);
        }

        if (model.getRideHistory() == null) {
            entity.setRideHistory(null);
        } else {
            List<RideStatusHistoryEntity> history = new ArrayList<>(model.getRideHistory().size());
            for (RideStatusHistoryModel historyModel : model.getRideHistory()) {
                RideStatusHistoryEntity historyEntity = toRideStatusHistoryEntity(historyModel, context);
                if (historyEntity != null) {
                    historyEntity.setRide(entity);
                }
                history.add(historyEntity);
            }
            entity.setRideHistory(history);
        }
        return entity;
    }

    private RideModel toRideModel(RideEntity entity, Map<Object, Object> context) {
        if (entity == null) {
            return null;
        }
        RideModel mapped = (RideModel) context.get(entity);
        if (mapped != null) {
            return mapped;
        }

        RideModel model = RideModel.builder()
                .id(entity.getId())
                .version(entity.getVersion())
                .riderId(entity.getRiderId())
                .activeRiderId(entity.getActiveRiderId())
                .driverId(entity.getDriverId())
                .activeDriverId(entity.getActiveDriverId())
                .vehicleId(entity.getVehicleId())
                .rideTypeCode(entity.getRideTypeCode())
                .currentStatus(entity.getCurrentStatus())
                .rideType(entity.getRideType())
                .pickupLatitude(entity.getPickupLatitude())
                .pickupLongitude(entity.getPickupLongitude())
                .destinationLatitude(entity.getDestinationLatitude())
                .destinationLongitude(entity.getDestinationLongitude())
                .estimatedDistance(entity.getEstimatedDistance())
                .actualDistance(entity.getActualDistance())
                .isCompleted(entity.getIsCompleted())
                .estimatedDuration(entity.getEstimatedDuration())
                .actualDuration(entity.getActualDuration())
                .requestedAt(entity.getRequestedAt())
                .acceptedAt(entity.getAcceptedAt())
                .driverArrivedAt(entity.getDriverArrivedAt())
                .startedAt(entity.getStartedAt())
                .isRideCancelled(entity.getIsRideCancelled())
                .completedAt(entity.getCompletedAt())
                .cancelledAt(entity.getCancelledAt())
                .cancelledBy(entity.getCancelledBy())
                .cancellationReason(entity.getCancellationReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
        context.put(entity, model);

        model.setOffer(toOfferModel(entity.getOffer(), context));

        FareDetailModel fareDetail = toFareDetailModel(entity.getFareDetail(), context);
        model.setFareDetail(fareDetail);
        if (fareDetail != null) {
            fareDetail.setRide(model);
        }

        if (entity.getRideHistory() == null) {
            model.setRideHistory(null);
        } else {
            List<RideStatusHistoryModel> history = new ArrayList<>(entity.getRideHistory().size());
            for (RideStatusHistoryEntity historyEntity : entity.getRideHistory()) {
                RideStatusHistoryModel historyModel = toRideStatusHistoryModel(historyEntity, context);
                if (historyModel != null) {
                    historyModel.setRide(model);
                }
                history.add(historyModel);
            }
            model.setRideHistory(history);
        }
        return model;
    }

    private FareDetailEntity toFareDetailEntity(FareDetailModel model, Map<Object, Object> context) {
        if (model == null) {
            return null;
        }
        FareDetailEntity mapped = (FareDetailEntity) context.get(model);
        if (mapped != null) {
            return mapped;
        }
        FareDetailEntity entity = FareDetailEntity.builder()
                .id(model.getId())
                .baseFare(model.getBaseFare())
                .distanceFare(model.getDistanceFare())
                .timeFare(model.getTimeFare())
                .bookingFee(model.getBookingFee())
                .tax(model.getTax())
                .surgeMultiplier(model.getSurgeMultiplier())
                .discount(model.getDiscount())
                .totalFare(model.getTotalFare())
                .currency(model.getCurrency())
                .paymentStatus(model.getPaymentStatus())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
        context.put(model, entity);
        entity.setRide(toRideEntity(model.getRide(), context));
        return entity;
    }

    private FareDetailModel toFareDetailModel(FareDetailEntity entity, Map<Object, Object> context) {
        if (entity == null) {
            return null;
        }
        FareDetailModel mapped = (FareDetailModel) context.get(entity);
        if (mapped != null) {
            return mapped;
        }
        FareDetailModel model = FareDetailModel.builder()
                .id(entity.getId())
                .baseFare(entity.getBaseFare())
                .distanceFare(entity.getDistanceFare())
                .timeFare(entity.getTimeFare())
                .bookingFee(entity.getBookingFee())
                .tax(entity.getTax())
                .surgeMultiplier(entity.getSurgeMultiplier())
                .discount(entity.getDiscount())
                .totalFare(entity.getTotalFare())
                .currency(entity.getCurrency())
                .paymentStatus(entity.getPaymentStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
        context.put(entity, model);
        model.setRide(toRideModel(entity.getRide(), context));
        return model;
    }

    private OfferEntity toOfferEntity(OfferModel model, Map<Object, Object> context) {
        if (model == null) {
            return null;
        }
        OfferEntity mapped = (OfferEntity) context.get(model);
        if (mapped != null) {
            return mapped;
        }
        OfferEntity entity = OfferEntity.builder()
                .id(model.getId())
                .offerCode(model.getOfferCode())
                .title(model.getTitle())
                .description(model.getDescription())
                .rideTypeCode(model.getRideTypeCode())
                .discountType(model.getDiscountType())
                .discountValue(model.getDiscountValue())
                .maximumDiscount(model.getMaximumDiscount())
                .minimumFare(model.getMinimumFare())
                .validFrom(model.getValidFrom())
                .validUntil(model.getValidUntil())
                .active(model.isActive())
                .usageLimit(model.getUsageLimit())
                .perUserLimit(model.getPerUserLimit())
                .usedCount(model.getUsedCount())
                .newUserOnly(model.isNewUserOnly())
                .stackable(model.isStackable())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
        context.put(model, entity);
        return entity;
    }

    private OfferModel toOfferModel(OfferEntity entity, Map<Object, Object> context) {
        if (entity == null) {
            return null;
        }
        OfferModel mapped = (OfferModel) context.get(entity);
        if (mapped != null) {
            return mapped;
        }
        OfferModel model = OfferModel.builder()
                .id(entity.getId())
                .offerCode(entity.getOfferCode())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .rideTypeCode(entity.getRideTypeCode())
                .discountType(entity.getDiscountType())
                .discountValue(entity.getDiscountValue())
                .maximumDiscount(entity.getMaximumDiscount())
                .minimumFare(entity.getMinimumFare())
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .active(entity.isActive())
                .usageLimit(entity.getUsageLimit())
                .perUserLimit(entity.getPerUserLimit())
                .usedCount(entity.getUsedCount())
                .newUserOnly(entity.isNewUserOnly())
                .stackable(entity.isStackable())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
        context.put(entity, model);
        return model;
    }

    private RideStatusHistoryEntity toRideStatusHistoryEntity(
            RideStatusHistoryModel model, Map<Object, Object> context) {
        if (model == null) {
            return null;
        }
        RideStatusHistoryEntity mapped = (RideStatusHistoryEntity) context.get(model);
        if (mapped != null) {
            return mapped;
        }
        RideStatusHistoryEntity entity = RideStatusHistoryEntity.builder()
                .id(model.getId())
                .rideStatus(model.getRideStatus())
                .changedById(model.getChangedById())
                .changedByType(model.getChangedByType())
                .remarks(model.getRemarks())
                .createdAt(model.getCreatedAt())
                .build();
        context.put(model, entity);
        entity.setRide(toRideEntity(model.getRide(), context));
        return entity;
    }

    private RideStatusHistoryModel toRideStatusHistoryModel(
            RideStatusHistoryEntity entity, Map<Object, Object> context) {
        if (entity == null) {
            return null;
        }
        RideStatusHistoryModel mapped = (RideStatusHistoryModel) context.get(entity);
        if (mapped != null) {
            return mapped;
        }
        RideStatusHistoryModel model = RideStatusHistoryModel.builder()
                .id(entity.getId())
                .rideStatus(entity.getRideStatus())
                .changedById(entity.getChangedById())
                .changedByType(entity.getChangedByType())
                .remarks(entity.getRemarks())
                .createdAt(entity.getCreatedAt())
                .build();
        context.put(entity, model);
        model.setRide(toRideModel(entity.getRide(), context));
        return model;
    }

    private RideDriverEntity toRideDriverEntity(RideDriverModel model, Map<Object, Object> context) {
        if (model == null) {
            return null;
        }
        RideDriverEntity mapped = (RideDriverEntity) context.get(model);
        if (mapped != null) {
            return mapped;
        }
        RideDriverEntity entity = RideDriverEntity.builder()
                .id(model.getId())
                .driverId(model.getDriverId())
                .authUserId(model.getAuthUserId())
                .driverName(model.getDriverName())
                .driverEmail(model.getDriverEmail())
                .driverPhone(model.getDriverPhone())
                .driverGender(model.getDriverGender())
                .driverProfileImage(model.getDriverProfileImage())
                .isActive(model.getIsActive())
                .rideDriverCreatedAt(model.getRideDriverCreatedAt())
                .rideDriverUpdatedAt(model.getRideDriverUpdatedAt())
                .build();
        context.put(model, entity);
        entity.setRide(toRideEntity(model.getRide(), context));
        return entity;
    }

    private RideDriverModel toRideDriverModel(RideDriverEntity entity, Map<Object, Object> context) {
        if (entity == null) {
            return null;
        }
        RideDriverModel mapped = (RideDriverModel) context.get(entity);
        if (mapped != null) {
            return mapped;
        }
        RideDriverModel model = RideDriverModel.builder()
                .id(entity.getId())
                .driverId(entity.getDriverId())
                .authUserId(entity.getAuthUserId())
                .driverName(entity.getDriverName())
                .driverEmail(entity.getDriverEmail())
                .driverPhone(entity.getDriverPhone())
                .driverGender(entity.getDriverGender())
                .driverProfileImage(entity.getDriverProfileImage())
                .isActive(entity.getIsActive())
                .rideDriverCreatedAt(entity.getRideDriverCreatedAt())
                .rideDriverUpdatedAt(entity.getRideDriverUpdatedAt())
                .build();
        context.put(entity, model);
        model.setRide(toRideModel(entity.getRide(), context));
        return model;
    }

    private RideTypesEntity toRideTypesEntity(RideTypesModel model, Map<Object, Object> context) {
        if (model == null) {
            return null;
        }
        RideTypesEntity mapped = (RideTypesEntity) context.get(model);
        if (mapped != null) {
            return mapped;
        }
        RideTypesEntity entity = RideTypesEntity.builder()
                .id(model.getId())
                .code(model.getCode())
                .name(model.getName())
                .description(model.getDescription())
                .rideType(model.getRideType())
                .active(model.isActive())
                .sortOrder(model.getSortOrder())
                .maxPassengers(model.getMaxPassengers())
                .scheduledSupported(model.isScheduledSupported())
                .cashSupported(model.isCashSupported())
                .onlinePaymentSupported(model.isOnlinePaymentSupported())
                .build();
        context.put(model, entity);
        return entity;
    }

    private RideTypesModel toRideTypesModel(RideTypesEntity entity, Map<Object, Object> context) {
        if (entity == null) {
            return null;
        }
        RideTypesModel mapped = (RideTypesModel) context.get(entity);
        if (mapped != null) {
            return mapped;
        }
        RideTypesModel model = RideTypesModel.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .rideType(entity.getRideType())
                .active(entity.isActive())
                .sortOrder(entity.getSortOrder())
                .maxPassengers(entity.getMaxPassengers())
                .scheduledSupported(entity.isScheduledSupported())
                .cashSupported(entity.isCashSupported())
                .onlinePaymentSupported(entity.isOnlinePaymentSupported())
                .build();
        context.put(entity, model);
        return model;
    }

    private RideUserEntity toRideUserEntity(RideUserModel model, Map<Object, Object> context) {
        if (model == null) {
            return null;
        }
        RideUserEntity mapped = (RideUserEntity) context.get(model);
        if (mapped != null) {
            return mapped;
        }
        RideUserEntity entity = new RideUserEntity();
        entity.setId(model.getId());
        entity.setAuthUserId(model.getAuthUserId());
        entity.setName(model.getName());
        entity.setPhoneNumber(model.getPhoneNumber());
        entity.setIsActive(model.getIsActive());
        entity.setRideCreatedAt(model.getRideCreatedAt());
        entity.setRideUpdatedAt(model.getRideUpdatedAt());
        context.put(model, entity);
        entity.setRide(toRideEntity(model.getRide(), context));
        return entity;
    }

    private RideUserModel toRideUserModel(RideUserEntity entity, Map<Object, Object> context) {
        if (entity == null) {
            return null;
        }
        RideUserModel mapped = (RideUserModel) context.get(entity);
        if (mapped != null) {
            return mapped;
        }
        RideUserModel model = new RideUserModel();
        model.setId(entity.getId());
        model.setAuthUserId(entity.getAuthUserId());
        model.setName(entity.getName());
        model.setPhoneNumber(entity.getPhoneNumber());
        model.setIsActive(entity.getIsActive());
        model.setRideCreatedAt(entity.getRideCreatedAt());
        model.setRideUpdatedAt(entity.getRideUpdatedAt());
        context.put(entity, model);
        model.setRide(toRideModel(entity.getRide(), context));
        return model;
    }
}
