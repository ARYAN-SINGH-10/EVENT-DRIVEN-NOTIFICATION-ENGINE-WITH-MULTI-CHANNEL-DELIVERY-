package com.notificationengine.repository;

import com.notificationengine.entity.TemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link TemplateVersion}.
 */
@Repository
public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, UUID> {

    List<TemplateVersion> findByTemplateIdOrderByVersionDesc(UUID templateId);

    Optional<TemplateVersion> findByTemplateIdAndVersion(UUID templateId, int version);

    @Query("SELECT MAX(tv.version) FROM TemplateVersion tv WHERE tv.templateId = :templateId")
    Optional<Integer> findMaxVersionByTemplateId(@Param("templateId") UUID templateId);

    long countByTemplateId(UUID templateId);

    void deleteByTemplateId(UUID templateId);
}
