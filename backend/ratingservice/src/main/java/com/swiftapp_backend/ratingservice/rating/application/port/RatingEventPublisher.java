package com.swiftapp_backend.ratingservice.rating.application.port;

public interface RatingEventPublisher {
    void publishRatingCreated(Object event);

    void publishRatingUpdated(Object event);

    void publishRatingDeleted(Object event);
}
