package com.swiftapp_backend.locationservice.location.application.port;

import java.util.List;

public interface LocationCachePort {
    void append(String key, Object value);

    boolean exists(String key);

    List<Object> getList(String key);

    void delete(String key);

    void addGeoLocation(String key, String member, double latitude, double longitude);

    void removeGeoLocation(String key, String member);

    List<String> findGeoMembersWithin(String key, double latitude, double longitude, double radiusKm);
}
