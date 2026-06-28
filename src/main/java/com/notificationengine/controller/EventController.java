package com.notificationengine.controller;

import com.notificationengine.dto.EventIngestionResponse;
import com.notificationengine.dto.EventPayload;
import com.notificationengine.entity.NotificationEvent;
import com.notificationengine.service.NotificationEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Event Ingestion API", description = "Endpoints for ingesting incoming financial events")
public class EventController {

    private final NotificationEventService eventService;

    @PostMapping
    @Operation(summary = "Ingest a new financial event")
    public ResponseEntity<EventIngestionResponse> ingestEvent(@Valid @RequestBody EventPayload payload) {
        NotificationEvent event = eventService.ingestEvent(payload);

        EventIngestionResponse response = EventIngestionResponse.builder()
                .eventId(event.getId())
                .correlationId(event.getCorrelationId())
                .eventType(event.getEventType())
                .status("ACCEPTED")
                .message("Event accepted for processing and publishing")
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
