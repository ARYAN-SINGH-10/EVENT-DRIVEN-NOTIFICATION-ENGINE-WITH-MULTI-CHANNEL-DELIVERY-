package com.notificationengine.service;

import com.notificationengine.dto.RenderedTemplate;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.TemplateVersion;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * High-level Template Engine Service supporting:
 * <ul>
 *   <li>Locale-aware template resolution with English fallback</li>
 *   <li>Handlebars-style rendering: {{var}}, {{#if}}, {{#each}}</li>
 *   <li>Automatic version snapshotting on update</li>
 *   <li>Version history and rollback</li>
 * </ul>
 */
public interface TemplateEngineService {

    /**
     * Resolves the best template for the given event/channel/locale and renders it.
     *
     * @param eventType  the financial event code (e.g. "TXNX-001")
     * @param channel    the delivery channel
     * @param locale     ISO locale code (e.g. "hi", "en")
     * @param variables  variable values for substitution
     * @return the fully rendered template
     */
    RenderedTemplate render(String eventType, ChannelType channel, String locale, Map<String, String> variables);

    /**
     * Renders a template by its exact database ID.
     */
    RenderedTemplate renderById(UUID templateId, Map<String, String> variables);

    /**
     * Returns the complete version history for a template, newest first.
     */
    List<TemplateVersion> getVersionHistory(UUID templateId);

    /**
     * Rolls back the live template to the content captured in the given version snapshot.
     * The rollback itself is recorded as a new version.
     *
     * @param templateId the template to roll back
     * @param version    the target version number to restore
     */
    void rollback(UUID templateId, int version);

    /**
     * Takes a version snapshot of the given template and persists it.
     *
     * @param templateId    the template being snapshotted
     * @param changeSummary a brief description of what changed
     */
    void snapshotVersion(UUID templateId, String changeSummary);
}
