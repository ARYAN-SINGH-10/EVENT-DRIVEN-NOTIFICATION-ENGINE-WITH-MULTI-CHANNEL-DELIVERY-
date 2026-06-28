package com.notificationengine.controller;

import com.notificationengine.dto.DashboardResponse;
import com.notificationengine.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard API", description = "Backend Dashboard Aggregation APIs")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get aggregated dashboard statistics", description = "Combines Analytics, DLQ, and User Preference data into a single comprehensive dashboard view.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dashboard statistics retrieved successfully")
    public ResponseEntity<DashboardResponse> getDashboardStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Start date (inclusive), defaults to 30 days ago", example = "2026-06-01") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "End date (inclusive), defaults to today", example = "2026-06-28") LocalDate to) {
        
        if (to == null) {
            to = LocalDate.now();
        }
        if (from == null) {
            from = to.minusDays(30);
        }

        return ResponseEntity.ok(dashboardService.getDashboardStats(from, to));
    }
}
