package com.swiftapp_backend.websocketservice.realtime.api.controller;

import com.swiftapp_backend.websocketservice.realtime.api.dto.ChatSessionDto;
import com.swiftapp_backend.websocketservice.realtime.application.service.ChatEventService;
import com.swiftapp_backend.websocketservice.realtime.domain.event.ChatSessionCreateEvent;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.config.RideChatSessionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatSessionApiController {
    private final ChatEventService chatEventService;
    private final RideChatSessionClient rideChatSessionClient;

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionDto> createSession(
            @RequestHeader("x-user-id") Long authUserId, @RequestBody ChatSessionDto dto) {
        RideChatSessionClient.Context ride = rideChatSessionClient.validate(authUserId, dto);
        dto.setDriverId(ride.driverId());
        dto.setRiderId(ride.riderId());
        dto.setRideId(ride.rideId());
        dto.setRideStatus(ride.rideStatus());
        chatEventService.createSession(ChatSessionCreateEvent.builder()
                .sessionNameForDriver(dto.getSessionNameForDriver())
                .sessionNameForRider(dto.getSessionNameForRider())
                .driverId(dto.getDriverId())
                .riderId(dto.getRiderId())
                .rideId(dto.getRideId())
                .rideStatus(dto.getRideStatus())
                .build());
        return ResponseEntity.accepted().body(dto);
    }
}
