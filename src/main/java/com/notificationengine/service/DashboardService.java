package com.notificationengine.service;

import com.notificationengine.dto.DashboardResponse;

import java.time.LocalDate;

/**
 * Service for aggregating global dashboard statistics.
 */
public interface DashboardService {
    
    /**
     * Retrieves the aggregated dashboard response for the given date range.
     * Integrates analytics, DLQ stats, and user preferences.
     */
    DashboardResponse getDashboardStats(LocalDate from, LocalDate to);
}
