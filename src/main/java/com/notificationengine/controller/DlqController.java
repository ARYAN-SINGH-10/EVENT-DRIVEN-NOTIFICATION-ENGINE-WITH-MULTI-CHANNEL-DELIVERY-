package com.notificationengine.controller;

import com.notificationengine.dto.DlqResponse;
import com.notificationengine.dto.DlqStatsResponse;
import com.notificationengine.dto.PagedResponse;
import com.notificationengine.entity.DeadLetterQueueEntry.DlqStatus;
import com.notificationengine.service.DlqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for Dead Letter Queue management and monitoring.
 */
@RestController
@RequestMapping("/api/v1/dlq")
@RequiredArgsConstructor
@Tag(name = "Dead Letter Queue", description = "Endpoints for managing and monitoring failed notifications in the DLQ")
public class DlqController {

    private final DlqService dlqService;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @GetMapping
    @Operation(summary = "List all DLQ entries", description = "Retrieves a paginated list of DLQ entries with optional status filtering.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of DLQ entries")
    public ResponseEntity<PagedResponse<DlqResponse>> getAll(
            @RequestParam(required = false) DlqStatus status, Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(dlqService.getByStatus(status, pageable));
        }
        return ResponseEntity.ok(dlqService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a DLQ entry by its ID", description = "Retrieves the full details of a specific dead letter queue entry by its UUID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DLQ entry found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "DLQ entry not found")
    public ResponseEntity<DlqResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(dlqService.getById(id));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "List DLQ entries by status", description = "Retrieves a paginated list of DLQ entries filtered by their current status (e.g., PENDING_RETRY, FAILED, RESOLVED).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of DLQ entries")
    public ResponseEntity<PagedResponse<DlqResponse>> getByStatus(
            @PathVariable DlqStatus status, Pageable pageable) {
        return ResponseEntity.ok(dlqService.getByStatus(status, pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List all DLQ entries for a specific user", description = "Retrieves all dead letter queue entries associated with a given user ID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of user's DLQ entries")
    public ResponseEntity<List<DlqResponse>> getByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(dlqService.getByUserId(userId));
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    @GetMapping("/stats")
    @Operation(summary = "Get aggregate DLQ monitoring statistics",
               description = "Returns counts by status, counts by channel type (for PENDING_RETRY), and entries currently due for retry.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DLQ statistics retrieved")
    public ResponseEntity<DlqStatsResponse> getStats() {
        return ResponseEntity.ok(dlqService.getStats());
    }

    // -------------------------------------------------------------------------
    // State Mutations
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Manually mark a DLQ entry as resolved", description = "Marks an entry as resolved so it will not be retried. Requires resolution notes and the resolving user's ID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DLQ entry successfully resolved")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "DLQ entry not found")
    public ResponseEntity<DlqResponse> resolve(
            @PathVariable UUID id,
            @RequestParam String resolvedBy,
            @RequestParam String notes) {
        return ResponseEntity.ok(dlqService.resolve(id, resolvedBy, notes));
    }

    @PostMapping("/{id}/discard")
    @Operation(summary = "Discard a DLQ entry permanently", description = "Permanently discards an entry so it will not be retried or replayed. Requires a discard reason.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DLQ entry successfully discarded")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "DLQ entry not found")
    public ResponseEntity<DlqResponse> discard(
            @PathVariable UUID id,
            @RequestParam String reason) {
        return ResponseEntity.ok(dlqService.discard(id, reason));
    }

    @PostMapping("/{id}/replay")
    @Operation(summary = "Manually replay a specific DLQ entry",
               description = "Resets the original notification to PENDING and re-dispatches it asynchronously via the channel processor.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DLQ entry successfully replayed")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "DLQ entry not found")
    public ResponseEntity<DlqResponse> replay(@PathVariable UUID id) {
        return ResponseEntity.ok(dlqService.replay(id));
    }

    // -------------------------------------------------------------------------
    // Bulk Operations
    // -------------------------------------------------------------------------

    @PostMapping("/retry-all")
    @Operation(summary = "Trigger immediate retry for all eligible entries",
               description = "Normally handled automatically by the RetryScheduler. Use this endpoint to force a retry cycle for all PENDING_RETRY entries without waiting for the next scheduler tick.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Number of entries retried")
    public ResponseEntity<Integer> retryAll() {
        return ResponseEntity.ok(dlqService.retryAll());
    }
}
