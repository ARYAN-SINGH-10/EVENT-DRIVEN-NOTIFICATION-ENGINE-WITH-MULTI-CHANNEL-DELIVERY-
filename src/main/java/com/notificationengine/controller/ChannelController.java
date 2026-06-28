package com.notificationengine.controller;

import com.notificationengine.dto.ChannelRequest;
import com.notificationengine.dto.ChannelResponse;
import com.notificationengine.service.ChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
@Tag(name = "Channel API", description = "Endpoints for managing notification channels")
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping
    @Operation(summary = "Create a new channel configuration")
    public ResponseEntity<ChannelResponse> create(@Valid @RequestBody ChannelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(channelService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing channel configuration")
    public ResponseEntity<ChannelResponse> update(@PathVariable UUID id, @Valid @RequestBody ChannelRequest request) {
        return ResponseEntity.ok(channelService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get channel by ID")
    public ResponseEntity<ChannelResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(channelService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Get all channels")
    public ResponseEntity<List<ChannelResponse>> getAll() {
        return ResponseEntity.ok(channelService.getAll());
    }

    @GetMapping("/active")
    @Operation(summary = "Get active channels")
    public ResponseEntity<List<ChannelResponse>> getActive() {
        return ResponseEntity.ok(channelService.getActive());
    }

    @PostMapping("/{id}/toggle")
    @Operation(summary = "Toggle channel active status")
    public ResponseEntity<ChannelResponse> toggleActive(@PathVariable UUID id, @RequestParam boolean active) {
        return ResponseEntity.ok(channelService.toggleActive(id, active));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a channel")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        channelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
