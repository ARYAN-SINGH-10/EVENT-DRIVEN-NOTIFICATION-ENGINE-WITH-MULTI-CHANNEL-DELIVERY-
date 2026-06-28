package com.notificationengine.scheduler;

import com.notificationengine.compliance.DataRetentionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for data retention and GDPR compliance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceScheduler {

    private final DataRetentionManager dataRetentionManager;

    /**
     * Purges expired notifications and audit logs based on retention policies.
     * Runs daily at 02:00 AM.
     */
    @Scheduled(cron = "${app.scheduler.retention-purge-cron:0 0 2 * * ?}")
    public void purgeExpiredData() {
        log.info("Running scheduled task: purgeExpiredData");
        try {
            dataRetentionManager.purgeExpiredNotifications();
            dataRetentionManager.purgeExpiredAuditLogs();
        } catch (Exception e) {
            log.error("Error in purgeExpiredData scheduled task: {}", e.getMessage());
        }
    }

    /**
     * Processes pending GDPR deletion requests.
     * Runs daily at 03:00 AM.
     */
    @Scheduled(cron = "${app.scheduler.gdpr-deletion-cron:0 0 3 * * ?}")
    public void processGdprDeletions() {
        log.info("Running scheduled task: processGdprDeletions");
        try {
            dataRetentionManager.processDeletionRequests();
        } catch (Exception e) {
            log.error("Error in processGdprDeletions scheduled task: {}", e.getMessage());
        }
    }
}
