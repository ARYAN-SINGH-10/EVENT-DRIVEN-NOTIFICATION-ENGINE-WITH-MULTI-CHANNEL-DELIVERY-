package com.notificationengine.repository;

import com.notificationengine.entity.ComplianceRecord;
import com.notificationengine.entity.ComplianceRecord.ConsentStatus;
import com.notificationengine.entity.ComplianceRecord.ConsentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ComplianceRecord}.
 */
@Repository
public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecord, UUID> {

    List<ComplianceRecord> findByUserId(String userId);

    Optional<ComplianceRecord> findByUserIdAndConsentType(String userId, ConsentType consentType);

    List<ComplianceRecord> findByUserIdAndConsentStatus(String userId, ConsentStatus status);

    Page<ComplianceRecord> findByConsentStatus(ConsentStatus status, Pageable pageable);

    @Query("SELECT c FROM ComplianceRecord c WHERE c.dataRetentionUntil < :now " +
           "AND c.consentStatus = 'GRANTED'")
    List<ComplianceRecord> findExpiredRetentionRecords(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM ComplianceRecord c WHERE c.consentStatus = 'DELETION_REQUESTED' " +
           "AND c.deletionCompletedAt IS NULL")
    List<ComplianceRecord> findPendingDeletionRequests();

    boolean existsByUserIdAndConsentTypeAndConsentStatus(
            String userId, ConsentType consentType, ConsentStatus status);

    @Modifying
    @Query("UPDATE ComplianceRecord c SET c.consentStatus = 'REVOKED', " +
           "c.consentRevokedAt = :revokedAt WHERE c.userId = :userId")
    int revokeAllConsentsForUser(@Param("userId") String userId, @Param("revokedAt") LocalDateTime revokedAt);

    void deleteByUserId(String userId);
}
