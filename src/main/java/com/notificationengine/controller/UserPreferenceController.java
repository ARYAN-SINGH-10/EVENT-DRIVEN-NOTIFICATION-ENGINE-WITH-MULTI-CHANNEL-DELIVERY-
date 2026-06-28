package com.notificationengine.controller;

import com.notificationengine.dto.UserPreferenceRequest;
import com.notificationengine.dto.UserPreferenceResponse;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.notificationengine.dto.ResolvedPreference;
import com.notificationengine.service.PreferenceResolutionService;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@Tag(name = "User Preference API", description = "Endpoints for managing user notification preferences")
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;
    private final PreferenceResolutionService preferenceResolutionService;

    @PutMapping
    @Operation(summary = "Create or update user preferences", description = "Sets up or updates global/channel preferences for a user, including timezone, opt-outs, and quiet hours.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Preference created or updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid preference payload")
    public ResponseEntity<UserPreferenceResponse> createOrUpdate(@Valid @RequestBody UserPreferenceRequest request) {
        return ResponseEntity.ok(preferenceService.createOrUpdate(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get preference by ID", description = "Retrieves a single preference record using its UUID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Preference found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Preference not found")
    public ResponseEntity<UserPreferenceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(preferenceService.getById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all preferences for a user", description = "Returns a list of all channel-specific preferences configured for the given user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of user preferences")
    public ResponseEntity<List<UserPreferenceResponse>> getByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(preferenceService.getByUserId(userId));
    }

    @GetMapping("/user/{userId}/channel/{channelType}")
    @Operation(summary = "Get preference for a specific user and channel", description = "Retrieves the user's preference settings for a particular delivery channel (e.g. SMS, EMAIL).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Preference found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Preference not found")
    public ResponseEntity<UserPreferenceResponse> getByUserIdAndChannel(
            @PathVariable String userId, @PathVariable ChannelType channelType) {
        return ResponseEntity.ok(preferenceService.getByUserIdAndChannel(userId, channelType));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a preference by ID", description = "Hard deletes a preference record by its UUID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Preference successfully deleted")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        preferenceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Delete all preferences for a user", description = "Hard deletes all preference configurations for a specified user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Preferences successfully deleted")
    public ResponseEntity<Void> deleteByUserId(@PathVariable String userId) {
        preferenceService.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resolve")
    @Operation(summary = "Resolve the effective preference for a user, event, and channel", description = "Simulates the preference resolution pipeline by evaluating regulatory overrides, user preferences, segment overrides, and system defaults to determine final eligibility.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Effective preference resolved")
    public ResponseEntity<ResolvedPreference> resolvePreference(
            @RequestParam String userId,
            @RequestParam String eventType,
            @RequestParam ChannelType channelType) {
        return ResponseEntity.ok(preferenceResolutionService.resolvePreference(userId, eventType, channelType));
    }
}
