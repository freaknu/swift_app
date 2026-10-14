package com.swiftapp_backend.riderservice.ride.domain.enums;

import java.util.List;
import java.util.Locale;

/** Exact vehicle codes offered by the driver service and understood by matching. */
public enum VehicleTypeCode {
    BIKE(RideType.BIKE, "BIKE", 1),
    SCOOTER(RideType.SCOOTER, "SCOOTER", 1),
    AUTO_RICKSHAW(RideType.AUTO, "AUTO", 3),
    HATCHBACK(RideType.STANDARD, "STANDARD", 4),
    SEDAN(RideType.STANDARD, "STANDARD", 4),
    SUV(RideType.SUV, "SUV", 6),
    LUXURY(RideType.LUXURY, "LUXURY", 4),
    VAN(RideType.SUV, "SUV", 6),
    ELECTRIC_BIKE(RideType.ELECTRIC, "BIKE", 1),
    ELECTRIC_CAR(RideType.ELECTRIC, "ELECTRIC", 4),
    CARPOOL(RideType.POOL, "POOL", 4),
    PREMIUM(RideType.PREMIUM, "PREMIUM", 4),
    PREMIUM_SUV(RideType.PREMIUM, "SUV", 6),
    DELIVERY_BIKE(RideType.DELIVERY, "BIKE", 1),
    DELIVERY_VAN(RideType.DELIVERY, "SUV", 2);

    private final RideType storedRideType;
    private final String fareCode;
    private final int maxPassengers;

    VehicleTypeCode(RideType storedRideType, String fareCode, int maxPassengers) {
        this.storedRideType = storedRideType;
        this.fareCode = fareCode;
        this.maxPassengers = maxPassengers;
    }

    public RideType storedRideType() {
        return storedRideType;
    }

    public String fareCode() {
        return fareCode;
    }

    public int maxPassengers() {
        return maxPassengers;
    }

    public String displayName() {
        String[] words = name().toLowerCase(Locale.ROOT).split("_");
        for (int i = 0; i < words.length; i++) {
            words[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);
        }
        return String.join(" ", words);
    }

    /** Supports older marketing codes while returning one precise vehicle category. */
    public static VehicleTypeCode fromRequest(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Vehicle type is required");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "AUTO" -> AUTO_RICKSHAW;
            case "STANDARD", "AIRPORT", "OUTSTATION", "RENTAL", "INTERCITY" -> SEDAN;
            case "ELECTRIC" -> ELECTRIC_CAR;
            case "POOL" -> CARPOOL;
            case "DELIVERY" -> DELIVERY_BIKE;
            default -> valueOf(normalized);
        };
    }

    public static List<VehicleTypeCode> catalog() {
        return List.of(values());
    }
}
