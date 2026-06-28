package com.notificationengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Monitoring statistics for the Dead Letter Queue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DlqStatsResponse {

    /** Total entries in DLQ across all statuses. */
    private long totalEntries;

    /** Counts broken down per DlqStatus. */
    private Map<String, Long> countsByStatus;

    /** Counts by channel type (for PENDING_RETRY entries). */
    private Map<String, Long> pendingRetryByChannel;

    /** Number of entries eligible for retry right now. */
    private long dueForRetry;

    /** Timestamp of this stats snapshot. */
    private LocalDateTime snapshotAt;
}
