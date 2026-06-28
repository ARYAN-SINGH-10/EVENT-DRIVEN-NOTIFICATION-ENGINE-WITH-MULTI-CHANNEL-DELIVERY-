package com.notificationengine.controller;

import com.notificationengine.dto.ComplianceRequest;
import com.notificationengine.dto.ComplianceResponse;
import com.notificationengine.dto.PagedResponse;
import com.notificationengine.entity.ComplianceRecord.ConsentType;
import com.notificationengine.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
@Tag(name = "Compliance API", description = "Endpoints for GDPR consent and data retention management")
public class ComplianceController {

    private final ComplianceService complianceService;

    @PostMapping("/consent")
    @Operation(summary = "Record or update user consent")
    public ResponseEntity<ComplianceResponse> recordConsent(@Valid @RequestBody ComplianceRequest request) {
        return ResponseEntity.ok(complianceService.recordConsent(request));
    }

    @GetMapping("/consent/{id}")
    @Operation(summary = "Get consent record by ID")
    public ResponseEntity<ComplianceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(complianceService.getById(id));
    }

    @GetMapping("/consent/user/{userId}")
    @Operation(summary = "Get all consent records for a user")
    public ResponseEntity<List<ComplianceResponse>> getByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(complianceService.getByUserId(userId));
    }

    @GetMapping("/consent/user/{userId}/check")
    @Operation(summary = "Check if user has granted a specific consent type")
    public ResponseEntity<Boolean> hasConsent(
            @PathVariable String userId, @RequestParam ConsentType consentType) {
        return ResponseEntity.ok(complianceService.hasConsent(userId, consentType));
    }

    @DeleteMapping("/consent/user/{userId}/revoke-all")
    @Operation(summary = "Revoke all consents for a user (GDPR right to object/erasure)")
    public ResponseEntity<Void> revokeAllConsents(@PathVariable String userId) {
        complianceService.revokeAllConsents(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/deletions/pending")
    @Operation(summary = "Get pending GDPR deletion requests")
    public ResponseEntity<PagedResponse<ComplianceResponse>> getPendingDeletions(Pageable pageable) {
        return ResponseEntity.ok(complianceService.getPendingDeletions(pageable));
    }
}
