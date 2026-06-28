package com.notificationengine.controller;

import com.notificationengine.dto.RenderedTemplate;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.TemplateVersion;
import com.notificationengine.service.TemplateEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/template-engine")
@RequiredArgsConstructor
@Tag(name = "Template Engine API", description = "Render, preview, and manage template versions")
public class TemplateEngineController {

    private final TemplateEngineService templateEngineService;

    // -------------------------------------------------------------------------
    // Render — locale-aware resolution
    // -------------------------------------------------------------------------

    @PostMapping("/render")
    @Operation(summary = "Render a template for a given event type, channel, and locale")
    public ResponseEntity<RenderedTemplate> render(@RequestBody RenderRequest request) {
        RenderedTemplate result = templateEngineService.render(
                request.getEventType(),
                request.getChannelType(),
                request.getLocale(),
                request.getVariables()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/render/{templateId}")
    @Operation(summary = "Render a specific template by ID")
    public ResponseEntity<RenderedTemplate> renderById(
            @PathVariable UUID templateId,
            @RequestBody(required = false) Map<String, String> variables) {
        return ResponseEntity.ok(templateEngineService.renderById(templateId, variables != null ? variables : Map.of()));
    }

    // -------------------------------------------------------------------------
    // Version History
    // -------------------------------------------------------------------------

    @GetMapping("/templates/{templateId}/versions")
    @Operation(summary = "Get complete version history for a template, newest first")
    public ResponseEntity<List<TemplateVersion>> getVersionHistory(@PathVariable UUID templateId) {
        return ResponseEntity.ok(templateEngineService.getVersionHistory(templateId));
    }

    @PostMapping("/templates/{templateId}/rollback")
    @Operation(summary = "Roll back a template to a specific version")
    public ResponseEntity<Void> rollback(
            @PathVariable UUID templateId,
            @RequestParam int version) {
        templateEngineService.rollback(templateId, version);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/templates/{templateId}/snapshot")
    @Operation(summary = "Manually create a version snapshot for a template")
    public ResponseEntity<Void> snapshot(
            @PathVariable UUID templateId,
            @RequestParam(defaultValue = "Manual snapshot") String summary) {
        templateEngineService.snapshotVersion(templateId, summary);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Request DTO
    // -------------------------------------------------------------------------

    @Data
    public static class RenderRequest {
        @NotBlank
        private String eventType;
        @NotNull
        private ChannelType channelType;
        private String locale = "en";
        private Map<String, String> variables = Map.of();
    }
}
