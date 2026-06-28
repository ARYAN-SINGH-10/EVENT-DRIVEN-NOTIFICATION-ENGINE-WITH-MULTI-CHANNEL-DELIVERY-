package com.notificationengine.controller;

import com.notificationengine.dto.taxonomy.FinancialEventTypeDto;
import com.notificationengine.service.FinancialEventTaxonomyService;
import com.notificationengine.taxonomy.FinancialEventCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/taxonomy/events")
@RequiredArgsConstructor
@Tag(name = "Financial Event Taxonomy API", description = "Endpoints for exploring the financial event taxonomy")
public class FinancialEventTaxonomyController {

    private final FinancialEventTaxonomyService taxonomyService;

    @GetMapping
    @Operation(summary = "Get all financial event types")
    public ResponseEntity<List<FinancialEventTypeDto>> getAllEvents() {
        return ResponseEntity.ok(taxonomyService.getAllEvents());
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get events by category")
    public ResponseEntity<List<FinancialEventTypeDto>> getEventsByCategory(@PathVariable FinancialEventCategory category) {
        return ResponseEntity.ok(taxonomyService.getEventsByCategory(category));
    }

    @GetMapping("/{eventCode}")
    @Operation(summary = "Get event details by its code")
    public ResponseEntity<FinancialEventTypeDto> getEventByCode(@PathVariable String eventCode) {
        return ResponseEntity.ok(taxonomyService.getEventByCode(eventCode));
    }
}
