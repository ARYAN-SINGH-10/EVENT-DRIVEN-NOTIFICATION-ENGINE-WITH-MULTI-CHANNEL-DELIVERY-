package com.notificationengine.controller;

import com.notificationengine.dto.PersonalizedPayload;
import com.notificationengine.service.PersonalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/personalization")
@RequiredArgsConstructor
@Tag(name = "Personalization API", description = "Inspect and preview the personalization pipeline output")
public class PersonalizationController {

    private final PersonalizationService personalizationService;

    @GetMapping("/{userId}")
    @Operation(summary = "Resolve the full personalization profile for a user")
    public ResponseEntity<PersonalizedPayload> resolveProfile(
            @PathVariable String userId,
            @RequestParam(defaultValue = "TXNX-001") String eventType) {
        return ResponseEntity.ok(
                personalizationService.personalize(userId, eventType, Map.of())
        );
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview the personalized payload for a user + event + variable combination")
    public ResponseEntity<PersonalizedPayload> preview(@RequestBody PreviewRequest request) {
        PersonalizedPayload result = personalizationService.personalize(
                request.getUserId(),
                request.getEventType(),
                request.getVariables() != null ? request.getVariables() : Map.of()
        );
        return ResponseEntity.ok(result);
    }

    @Data
    public static class PreviewRequest {
        @NotBlank
        private String userId;
        @NotBlank
        private String eventType;
        private Map<String, String> variables;
    }
}
