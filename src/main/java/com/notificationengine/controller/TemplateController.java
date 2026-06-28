package com.notificationengine.controller;

import com.notificationengine.dto.PagedResponse;
import com.notificationengine.dto.TemplateRequest;
import com.notificationengine.dto.TemplateResponse;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.TemplateVersion;
import com.notificationengine.service.TemplateEngineService;
import com.notificationengine.service.TemplateService;
import com.notificationengine.template.TemplateLocale;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Template API", description = "Endpoints for managing notification templates")
public class TemplateController {

    private final TemplateService templateService;
    private final TemplateEngineService templateEngineService;

    @PostMapping
    @Operation(summary = "Create a new template")
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing template (auto-creates a version snapshot)")
    public ResponseEntity<TemplateResponse> update(@PathVariable UUID id, @Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.ok(templateService.update(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID")
    public ResponseEntity<TemplateResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.getById(id));
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get template by name")
    public ResponseEntity<TemplateResponse> getByName(@PathVariable String name) {
        return ResponseEntity.ok(templateService.getByName(name));
    }

    @GetMapping
    @Operation(summary = "Get all templates (paginated)")
    public ResponseEntity<PagedResponse<TemplateResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(templateService.getAll(pageable));
    }

    @GetMapping("/channel/{channelType}")
    @Operation(summary = "Get templates by channel")
    public ResponseEntity<PagedResponse<TemplateResponse>> getByChannel(
            @PathVariable ChannelType channelType, Pageable pageable) {
        return ResponseEntity.ok(templateService.getByChannel(channelType, pageable));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a template")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a template")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        templateService.deactivate(id);
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // Version Management
    // -------------------------------------------------------------------------

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get complete version history for a template, newest first")
    public ResponseEntity<List<TemplateVersion>> getVersionHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(templateEngineService.getVersionHistory(id));
    }

    @PostMapping("/{id}/rollback")
    @Operation(summary = "Roll back a template to a specific version number")
    public ResponseEntity<Map<String, String>> rollback(
            @PathVariable UUID id,
            @RequestParam int version) {
        templateEngineService.rollback(id, version);
        return ResponseEntity.ok(Map.of(
                "message", "Template rolled back to version " + version,
                "templateId", id.toString()
        ));
    }

    @PostMapping("/{id}/snapshot")
    @Operation(summary = "Manually create a version snapshot")
    public ResponseEntity<Map<String, String>> snapshot(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "Manual snapshot") String summary) {
        templateEngineService.snapshotVersion(id, summary);
        return ResponseEntity.ok(Map.of("message", "Snapshot created: " + summary));
    }

    // -------------------------------------------------------------------------
    // Localization helpers
    // -------------------------------------------------------------------------

    @GetMapping("/locales")
    @Operation(summary = "List all supported locale codes and their display names")
    public ResponseEntity<List<Map<String, String>>> getSupportedLocales() {
        List<Map<String, String>> locales = Arrays.stream(TemplateLocale.values())
                .map(l -> Map.of("code", l.getCode(), "name", l.getDisplayName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(locales);
    }
}

