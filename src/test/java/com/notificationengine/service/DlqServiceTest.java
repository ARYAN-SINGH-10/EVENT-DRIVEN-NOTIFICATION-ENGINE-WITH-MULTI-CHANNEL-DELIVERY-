package com.notificationengine.service;

import com.notificationengine.dlq.DeadLetterQueueHandler;
import com.notificationengine.dto.DlqResponse;
import com.notificationengine.dto.DlqStatsResponse;
import com.notificationengine.entity.DeadLetterQueueEntry;
import com.notificationengine.entity.DeadLetterQueueEntry.DlqStatus;
import com.notificationengine.entity.NotificationEntity;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import com.notificationengine.exception.DlqException;
import com.notificationengine.repository.DeadLetterQueueRepository;
import com.notificationengine.repository.NotificationRepository;
import com.notificationengine.service.impl.DlqServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DlqServiceImpl — covering queries, state mutations, replay, and stats.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DlqService — Dead Letter Queue Unit Tests")
class DlqServiceTest {

    @Mock DeadLetterQueueRepository dlqRepository;
    @Mock DeadLetterQueueHandler dlqHandler;
    @Mock NotificationRepository notificationRepository;
    @Mock NotificationService notificationService;

    private DlqServiceImpl dlqService;

    @BeforeEach
    void setUp() {
        dlqService = new DlqServiceImpl(dlqRepository, dlqHandler, notificationRepository, notificationService);
    }

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    private DeadLetterQueueEntry buildEntry(UUID id, DlqStatus status) {
        return DeadLetterQueueEntry.builder()
                .id(id)
                .notificationId(UUID.randomUUID())
                .userId("user-001")
                .channelType(ChannelType.SMS)
                .failureReason("Connection timeout")
                .failureCount(1)
                .maxRetryAttempts(5)
                .status(status)
                .nextRetryAt(LocalDateTime.now().minusMinutes(5))
                .lastAttemptAt(LocalDateTime.now().minusMinutes(10))
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private NotificationEntity buildNotification(UUID id) {
        NotificationEntity e = new NotificationEntity();
        e.setId(id);
        e.setUserId("user-001");
        e.setChannelType(ChannelType.SMS);
        e.setStatus(NotificationStatus.DLQ);
        e.setMaxRetries(3);
        e.setRetryCount(3);
        return e;
    }

    // =========================================================================
    // Query Tests
    // =========================================================================

    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("getById returns mapped DlqResponse when found")
        void getById_found_returnsMappedResponse() {
            UUID id = UUID.randomUUID();
            DeadLetterQueueEntry entry = buildEntry(id, DlqStatus.PENDING_RETRY);
            when(dlqRepository.findById(id)).thenReturn(Optional.of(entry));

            DlqResponse response = dlqService.getById(id);

            assertThat(response.getId()).isEqualTo(id);
            assertThat(response.getUserId()).isEqualTo("user-001");
            assertThat(response.getStatus()).isEqualTo(DlqStatus.PENDING_RETRY);
        }

        @Test
        @DisplayName("getById throws DlqException when not found")
        void getById_notFound_throwsDlqException() {
            UUID id = UUID.randomUUID();
            when(dlqRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dlqService.getById(id))
                    .isInstanceOf(DlqException.class);
        }

        @Test
        @DisplayName("getByStatus returns paged results")
        void getByStatus_returnsPaged() {
            UUID id = UUID.randomUUID();
            DeadLetterQueueEntry entry = buildEntry(id, DlqStatus.EXHAUSTED);
            PageRequest pageable = PageRequest.of(0, 10);
            when(dlqRepository.findByStatus(DlqStatus.EXHAUSTED, pageable))
                    .thenReturn(new PageImpl<>(List.of(entry)));

            var result = dlqService.getByStatus(DlqStatus.EXHAUSTED, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(DlqStatus.EXHAUSTED);
        }

        @Test
        @DisplayName("getByUserId returns all entries for a user")
        void getByUserId_returnsAllEntries() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            when(dlqRepository.findByUserIdOrderByCreatedAtDesc("user-001"))
                    .thenReturn(List.of(buildEntry(id1, DlqStatus.PENDING_RETRY), buildEntry(id2, DlqStatus.EXHAUSTED)));

            List<DlqResponse> responses = dlqService.getByUserId("user-001");

            assertThat(responses).hasSize(2);
        }
    }

    // =========================================================================
    // Mutation Tests
    // =========================================================================

    @Nested
    @DisplayName("State Mutation Tests")
    class MutationTests {

        @Test
        @DisplayName("resolve marks entry as resolved and delegates to dlqHandler")
        void resolve_updatesStatus() {
            UUID id = UUID.randomUUID();
            DeadLetterQueueEntry entry = buildEntry(id, DlqStatus.PENDING_RETRY);
            DeadLetterQueueEntry resolved = buildEntry(id, DlqStatus.RESOLVED);
            resolved.setResolvedBy("admin");
            resolved.setResolutionNotes("Fixed manually");
            resolved.setResolvedAt(LocalDateTime.now());

            when(dlqRepository.findById(id)).thenReturn(Optional.of(entry), Optional.of(resolved));

            DlqResponse response = dlqService.resolve(id, "admin", "Fixed manually");

            verify(dlqHandler).resolve(eq(id), eq("admin"), eq("Fixed manually"));
            assertThat(response.getStatus()).isEqualTo(DlqStatus.RESOLVED);
        }

        @Test
        @DisplayName("resolve throws DlqException when entry is already resolved")
        void resolve_alreadyResolved_throws() {
            UUID id = UUID.randomUUID();
            when(dlqRepository.findById(id)).thenReturn(Optional.of(buildEntry(id, DlqStatus.RESOLVED)));

            assertThatThrownBy(() -> dlqService.resolve(id, "admin", "notes"))
                    .isInstanceOf(DlqException.class)
                    .hasMessageContaining("already resolved");
        }

        @Test
        @DisplayName("discard sets status to DISCARDED and records reason")
        void discard_setsDiscardedStatus() {
            UUID id = UUID.randomUUID();
            DeadLetterQueueEntry entry = buildEntry(id, DlqStatus.PENDING_RETRY);
            when(dlqRepository.findById(id)).thenReturn(Optional.of(entry));
            when(dlqRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DlqResponse response = dlqService.discard(id, "No longer needed");

            assertThat(response.getStatus()).isEqualTo(DlqStatus.DISCARDED);
        }

        @Test
        @DisplayName("discard throws when entry is already discarded")
        void discard_alreadyDiscarded_throws() {
            UUID id = UUID.randomUUID();
            when(dlqRepository.findById(id)).thenReturn(Optional.of(buildEntry(id, DlqStatus.DISCARDED)));

            assertThatThrownBy(() -> dlqService.discard(id, "reason"))
                    .isInstanceOf(DlqException.class);
        }
    }

    // =========================================================================
    // Replay Tests
    // =========================================================================

    @Nested
    @DisplayName("Replay Tests")
    class ReplayTests {

        @Test
        @DisplayName("replay resets notification to PENDING and triggers reDispatch")
        void replay_resetsAndDispatches() {
            UUID dlqId = UUID.randomUUID();
            UUID notifId = UUID.randomUUID();
            DeadLetterQueueEntry entry = buildEntry(dlqId, DlqStatus.EXHAUSTED);
            entry.setNotificationId(notifId);

            NotificationEntity notification = buildNotification(notifId);
            DeadLetterQueueEntry updatedEntry = buildEntry(dlqId, DlqStatus.RETRYING);

            when(dlqRepository.findById(dlqId)).thenReturn(Optional.of(entry), Optional.of(updatedEntry));
            when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any())).thenReturn(notification);
            when(dlqRepository.save(any())).thenReturn(updatedEntry);

            DlqResponse response = dlqService.replay(dlqId);

            // Notification should have been reset to PENDING
            verify(notificationRepository).save(argThat(n -> n.getStatus() == NotificationStatus.PENDING));
            // reDispatch should have been called
            verify(notificationService).reDispatch(notifId);
            // Entry should be returned as RETRYING
            assertThat(response.getStatus()).isEqualTo(DlqStatus.RETRYING);
        }

        @Test
        @DisplayName("replay throws when DLQ entry is DISCARDED")
        void replay_discardedEntry_throws() {
            UUID id = UUID.randomUUID();
            when(dlqRepository.findById(id)).thenReturn(Optional.of(buildEntry(id, DlqStatus.DISCARDED)));

            assertThatThrownBy(() -> dlqService.replay(id))
                    .isInstanceOf(DlqException.class)
                    .hasMessageContaining("discarded");
        }

        @Test
        @DisplayName("replay throws when DLQ entry is already RESOLVED")
        void replay_resolvedEntry_throws() {
            UUID id = UUID.randomUUID();
            when(dlqRepository.findById(id)).thenReturn(Optional.of(buildEntry(id, DlqStatus.RESOLVED)));

            assertThatThrownBy(() -> dlqService.replay(id))
                    .isInstanceOf(DlqException.class)
                    .hasMessageContaining("resolved");
        }
    }

    // =========================================================================
    // Bulk Retry Tests
    // =========================================================================

    @Nested
    @DisplayName("Bulk Retry Tests")
    class RetryAllTests {

        @Test
        @DisplayName("retryAll returns 0 when no entries are ready")
        void retryAll_noneReady_returnsZero() {
            when(dlqHandler.getEntriesReadyForRetry()).thenReturn(List.of());

            int result = dlqService.retryAll();

            assertThat(result).isZero();
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("retryAll re-dispatches each eligible entry and returns count")
        void retryAll_multipleEntries_dispatchesAll() {
            UUID dlqId1 = UUID.randomUUID();
            UUID dlqId2 = UUID.randomUUID();
            UUID notifId1 = UUID.randomUUID();
            UUID notifId2 = UUID.randomUUID();

            DeadLetterQueueEntry e1 = buildEntry(dlqId1, DlqStatus.PENDING_RETRY);
            e1.setNotificationId(notifId1);
            DeadLetterQueueEntry e2 = buildEntry(dlqId2, DlqStatus.PENDING_RETRY);
            e2.setNotificationId(notifId2);

            NotificationEntity n1 = buildNotification(notifId1);
            NotificationEntity n2 = buildNotification(notifId2);

            when(dlqHandler.getEntriesReadyForRetry()).thenReturn(List.of(e1, e2));
            when(notificationRepository.findById(notifId1)).thenReturn(Optional.of(n1));
            when(notificationRepository.findById(notifId2)).thenReturn(Optional.of(n2));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(dlqRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            int result = dlqService.retryAll();

            assertThat(result).isEqualTo(2);
            verify(notificationService).reDispatch(notifId1);
            verify(notificationService).reDispatch(notifId2);
        }
    }

    // =========================================================================
    // Stats Tests
    // =========================================================================

    @Nested
    @DisplayName("Monitoring Stats Tests")
    class StatsTests {

        @Test
        @DisplayName("getStats returns counts for all DLQ statuses")
        void getStats_returnsAllStatusCounts() {
            for (DlqStatus s : DlqStatus.values()) {
                when(dlqRepository.countByStatus(s)).thenReturn(s == DlqStatus.PENDING_RETRY ? 7L : 0L);
            }
            when(dlqRepository.findEntriesReadyForRetry(any())).thenReturn(List.of());
            when(dlqRepository.countPendingByChannel(any())).thenReturn(0L);

            DlqStatsResponse stats = dlqService.getStats();

            assertThat(stats.getCountsByStatus()).containsKey("PENDING_RETRY");
            assertThat(stats.getCountsByStatus().get("PENDING_RETRY")).isEqualTo(7L);
            assertThat(stats.getTotalEntries()).isEqualTo(7L);
            assertThat(stats.getSnapshotAt()).isNotNull();
        }

        @Test
        @DisplayName("getStats reflects dueForRetry count correctly")
        void getStats_dueForRetry_isAccurate() {
            for (DlqStatus s : DlqStatus.values()) {
                when(dlqRepository.countByStatus(s)).thenReturn(0L);
            }
            when(dlqRepository.countPendingByChannel(any())).thenReturn(0L);

            UUID dlqId = UUID.randomUUID();
            when(dlqRepository.findEntriesReadyForRetry(any()))
                    .thenReturn(List.of(buildEntry(dlqId, DlqStatus.PENDING_RETRY)));

            DlqStatsResponse stats = dlqService.getStats();

            assertThat(stats.getDueForRetry()).isEqualTo(1L);
        }
    }
}
