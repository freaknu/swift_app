package com.swiftapp_backend.locationservice.eta.application.port.out;

import com.swiftapp_backend.locationservice.eta.domain.model.Eta;

/** Best-effort notification; publishing failure must not discard a valid ETA. */
public interface EtaEventPublisher {
    void publish(Eta eta);
}
