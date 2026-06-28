package com.notificationengine.controller;

import com.notificationengine.dto.NotificationRequest;
import com.notificationengine.dto.NotificationResponse;
import com.notificationengine.dto.PagedResponse;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import com.notificationengine.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification API", description = "Endpoints for managing and sending notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "Send a new notification", description = "Dispatches a new notification to the specified user through the chosen channel. Validates preferences, applies templates, and queues for delivery.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Notification successfully created and queued")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload")
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.send(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a notification by ID", description = "Fetches the full details of a specific notification by its UUID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    public ResponseEntity<NotificationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.getById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get notifications for a user", description = "Retrieves a paginated list of all notifications targeted at a specific user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of user notifications")
    public ResponseEntity<PagedResponse<NotificationResponse>> getByUserId(
            @PathVariable String userId, Pageable pageable) {
        return ResponseEntity.ok(notificationService.getByUserId(userId, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get notifications by status", description = "Retrieves a paginated list of notifications currently in the specified state (e.g. FAILED, DELIVERED).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of notifications matching the status")
    public ResponseEntity<PagedResponse<NotificationResponse>> getByStatus(
            @PathVariable NotificationStatus status, Pageable pageable) {
        return ResponseEntity.ok(notificationService.getByStatus(status, pageable));
    }

    @GetMapping("/channel/{channelType}")
    @Operation(summary = "Get notifications by channel type", description = "Retrieves a paginated list of notifications sent via a specific channel (e.g. SMS, EMAIL).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of notifications matching the channel")
    public ResponseEntity<PagedResponse<NotificationResponse>> getByChannelType(
            @PathVariable ChannelType channelType, Pageable pageable) {
        return ResponseEntity.ok(notificationService.getByChannelType(channelType, pageable));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a scheduled or pending notification", description = "Transitions a pending or scheduled notification to the CANCELLED state. Has no effect on already delivered messages.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification successfully cancelled")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    public ResponseEntity<NotificationResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.cancel(id));
    }
}
