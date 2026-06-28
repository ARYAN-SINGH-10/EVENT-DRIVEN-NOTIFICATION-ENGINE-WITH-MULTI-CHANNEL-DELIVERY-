package com.notificationengine.service.impl;

import com.notificationengine.dto.RenderedTemplate;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationTemplate;
import com.notificationengine.entity.TemplateVersion;
import com.notificationengine.exception.TemplateNotFoundException;
import com.notificationengine.repository.NotificationTemplateRepository;
import com.notificationengine.repository.TemplateVersionRepository;
import com.notificationengine.service.TemplateEngineService;
import com.notificationengine.template.TemplateEngine;
import com.notificationengine.template.TemplateLocale;
import com.notificationengine.template.TemplateVariableResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Full implementation of the Template Engine Service.
 *
 * <h3>Resolution Strategy</h3>
 * <ol>
 *   <li>Exact locale match (channelType + eventType + locale)</li>
 *   <li>English fallback (channelType + eventType + "en")</li>
 *   <li>Any active template for channelType + eventType</li>
 * </ol>
 *
 * <h3>Rendering Pipeline</h3>
 * <ol>
 *   <li>System variables injected (currentDate, appName, etc.)</li>
 *   <li>Handlebars blocks processed (#if, #unless, #each)</li>
 *   <li>Variable substitution applied</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateEngineServiceImpl implements TemplateEngineService {

    private final NotificationTemplateRepository templateRepository;
    private final TemplateVersionRepository versionRepository;
    private final TemplateEngine templateEngine;
    private final TemplateVariableResolver variableResolver;

    // -----------------------------------------------------------------------
    // render — locale-aware resolution + Handlebars rendering
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public RenderedTemplate render(String eventType, ChannelType channel, String locale, Map<String, String> variables) {
        String resolvedLocale = TemplateLocale.fromCode(locale).getCode();
        boolean fallback = false;

        // 1. Try exact locale
        Optional<NotificationTemplate> templateOpt = templateRepository
                .findByChannelTypeAndEventTypeAndLocaleAndActive(channel, eventType, resolvedLocale, true);

        // 2. Fallback: JPQL query returns exact-match first, then "en"
        if (templateOpt.isEmpty()) {
            List<NotificationTemplate> candidates = templateRepository
                    .findByChannelEventLocaleWithFallback(channel, eventType, resolvedLocale);
            if (!candidates.isEmpty()) {
                templateOpt = Optional.of(candidates.get(0));
                fallback = !resolvedLocale.equals(candidates.get(0).getLocale());
            }
        }

        // 3. Any active template for channel+event
        if (templateOpt.isEmpty()) {
            List<NotificationTemplate> any = templateRepository
                    .findAllByChannelTypeAndEventTypeAndActive(channel, eventType);
            if (!any.isEmpty()) {
                templateOpt = Optional.of(any.get(0));
                fallback = true;
            }
        }

        NotificationTemplate template = templateOpt.orElseThrow(() ->
                new TemplateNotFoundException(String.format("No template found for event=%s channel=%s locale=%s",
                        eventType, channel, resolvedLocale)));

        return doRender(template, variables, fallback);
    }

    @Override
    @Transactional(readOnly = true)
    public RenderedTemplate renderById(UUID templateId, Map<String, String> variables) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId.toString()));
        return doRender(template, variables, false);
    }

    // -----------------------------------------------------------------------
    // Version history
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "templateVersions", key = "#templateId")
    public List<TemplateVersion> getVersionHistory(UUID templateId) {
        if (!templateRepository.existsById(templateId)) {
            throw new TemplateNotFoundException(templateId.toString());
        }
        return versionRepository.findByTemplateIdOrderByVersionDesc(templateId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "templateVersions", key = "#templateId")
    public void rollback(UUID templateId, int targetVersion) {
        NotificationTemplate live = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId.toString()));

        TemplateVersion snap = versionRepository.findByTemplateIdAndVersion(templateId, targetVersion)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "Version " + targetVersion + " not found for template " + templateId));

        // Snapshot current state before overwrite
        snapshotVersionInternal(live, "Pre-rollback snapshot");

        // Restore snapshot content into live template
        live.setSubjectTemplate(snap.getSubjectTemplate());
        live.setBodyTemplate(snap.getBodyTemplate());
        live.setHtmlBodyTemplate(snap.getHtmlBodyTemplate());
        live.setVersion(live.getVersion() + 1);
        templateRepository.save(live);

        // Record rollback as a new version
        TemplateVersion rollbackVersion = TemplateVersion.builder()
                .templateId(templateId)
                .templateName(live.getName())
                .version(live.getVersion())
                .channelType(live.getChannelType())
                .eventType(live.getEventType())
                .locale(live.getLocale())
                .subjectTemplate(snap.getSubjectTemplate())
                .bodyTemplate(snap.getBodyTemplate())
                .htmlBodyTemplate(snap.getHtmlBodyTemplate())
                .changeSummary("Rolled back to version " + targetVersion)
                .build();
        versionRepository.save(rollbackVersion);

        log.info("Template {} rolled back to version {} (new version: {})", templateId, targetVersion, live.getVersion());
    }

    @Override
    @Transactional
    public void snapshotVersion(UUID templateId, String changeSummary) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException(templateId.toString()));
        snapshotVersionInternal(template, changeSummary);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves variables and renders all template fields through the engine pipeline.
     */
    private RenderedTemplate doRender(NotificationTemplate template, Map<String, String> variables, boolean localeFallback) {
        // Merge system vars + user-supplied vars
        Map<String, String> resolved = variableResolver.resolve(variables, null);

        // Compute which variables were used and which are missing
        Set<String> allVarsInTemplate = new LinkedHashSet<>();
        allVarsInTemplate.addAll(templateEngine.extractVariableNames(template.getSubjectTemplate()));
        allVarsInTemplate.addAll(templateEngine.extractVariableNames(template.getBodyTemplate()));
        allVarsInTemplate.addAll(templateEngine.extractVariableNames(template.getHtmlBodyTemplate()));

        Set<String> resolvedVars = new LinkedHashSet<>();
        Set<String> missingVars = new LinkedHashSet<>();
        for (String varName : allVarsInTemplate) {
            if (resolved.containsKey(varName) && StringUtils.isNotBlank(resolved.get(varName))) {
                resolvedVars.add(varName);
            } else {
                missingVars.add(varName);
            }
        }

        if (!missingVars.isEmpty()) {
            log.warn("Template '{}' rendered with missing variables: {}", template.getName(), missingVars);
        }

        String renderedSubject  = templateEngine.render(template.getSubjectTemplate(), resolved);
        String renderedBody     = templateEngine.render(template.getBodyTemplate(), resolved);
        String renderedHtmlBody = templateEngine.render(template.getHtmlBodyTemplate(), resolved);

        return RenderedTemplate.builder()
                .templateId(template.getId())
                .templateName(template.getName())
                .version(template.getVersion())
                .locale(template.getLocale())
                .localeFallback(localeFallback)
                .channelType(template.getChannelType())
                .subject(renderedSubject)
                .body(renderedBody)
                .htmlBody(renderedHtmlBody)
                .resolvedVariables(resolvedVars)
                .missingVariables(missingVars)
                .build();
    }

    private void snapshotVersionInternal(NotificationTemplate template, String changeSummary) {
        TemplateVersion snapshot = TemplateVersion.builder()
                .templateId(template.getId())
                .templateName(template.getName())
                .version(template.getVersion())
                .channelType(template.getChannelType())
                .eventType(template.getEventType())
                .locale(template.getLocale())
                .subjectTemplate(template.getSubjectTemplate())
                .bodyTemplate(template.getBodyTemplate())
                .htmlBodyTemplate(template.getHtmlBodyTemplate())
                .changeSummary(changeSummary)
                .build();
        versionRepository.save(snapshot);
        log.debug("Version snapshot saved for template={} version={}", template.getId(), template.getVersion());
    }
}
