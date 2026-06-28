package com.notificationengine.service;

import com.notificationengine.dto.DlqResponse;
import com.notificationengine.dto.DlqStatsResponse;
import com.notificationengine.dto.PagedResponse;
import com.notificationengine.entity.DeadLetterQueueEntry.DlqStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Dead Letter Queue management.
 */
public interface DlqService {

    PagedResponse<DlqResponse> getByStatus(DlqStatus status, Pageable pageable);

    DlqResponse getById(UUID id);

    List<DlqResponse> getByUserId(String userId);

    DlqResponse resolve(UUID id, String resolvedBy, String notes);

    DlqResponse discard(UUID id, String reason);

    DlqResponse replay(UUID id);

    int retryAll();

    /**
     * Returns aggregate monitoring stats across all DLQ statuses.
     */
    DlqStatsResponse getStats();
}
