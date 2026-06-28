package com.notificationengine.service.impl;

import com.notificationengine.dto.PagedResponse;
import com.notificationengine.dto.TemplateRequest;
import com.notificationengine.dto.TemplateResponse;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationTemplate;
import com.notificationengine.exception.ErrorCode;
import com.notificationengine.exception.NotificationException;
import com.notificationengine.exception.TemplateNotFoundException;
import com.notificationengine.repository.NotificationTemplateRepository;
import com.notificationengine.service.TemplateEngineService;
import com.notificationengine.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final TemplateEngineService templateEngineService;

    @Override
    @Transactional
    public TemplateResponse create(TemplateRequest request) {
        if (templateRepository.existsByName(request.getName())) {
            throw new NotificationException(ErrorCode.TEMPLATE_ALREADY_EXISTS,
                    "Template already exists: " + request.getName(), 409);
        }
        NotificationTemplate template = NotificationTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .channelType(request.getChannelType())
                .eventType(request.getEventType())
                .subjectTemplate(request.getSubjectTemplate())
                .bodyTemplate(request.getBodyTemplate())
                .htmlBodyTemplate(request.getHtmlBodyTemplate())
                .availableVariables(request.getAvailableVariables())
                .locale(request.getLocale())
                .active(request.isActive())
                .build();
        return toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    @CacheEvict(value = "templates", key = "#id")
    public TemplateResponse update(UUID id, TemplateRequest request) {
        NotificationTemplate template = getEntityById(id);
        // Snapshot BEFORE applying changes so history is preserved
        templateEngineService.snapshotVersion(id, "Pre-update snapshot (v" + template.getVersion() + ")");
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setSubjectTemplate(request.getSubjectTemplate());
        template.setBodyTemplate(request.getBodyTemplate());
        template.setHtmlBodyTemplate(request.getHtmlBodyTemplate());
        template.setAvailableVariables(request.getAvailableVariables());
        template.setLocale(request.getLocale());
        template.setActive(request.isActive());
        template.setVersion(template.getVersion() + 1);
        return toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "templates", key = "#id")
    public TemplateResponse getById(UUID id) {
        return toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "templates", key = "#name")
    public TemplateResponse getByName(String name) {
        return templateRepository.findByName(name)
                .map(this::toResponse)
                .orElseThrow(() -> new TemplateNotFoundException(name));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TemplateResponse> getAll(Pageable pageable) {
        Page<TemplateResponse> page = templateRepository.findAll(pageable).map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TemplateResponse> getByChannel(ChannelType channelType, Pageable pageable) {
        Page<TemplateResponse> page = templateRepository
                .findActiveTemplates(channelType, null, pageable)
                .map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional
    @CacheEvict(value = "templates", key = "#id")
    public void delete(UUID id) {
        if (!templateRepository.existsById(id)) throw new TemplateNotFoundException(id.toString());
        templateRepository.deleteById(id);
        log.info("Template deleted: id={}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "templates", key = "#id")
    public void deactivate(UUID id) {
        NotificationTemplate t = getEntityById(id);
        t.setActive(false);
        templateRepository.save(t);
        log.info("Template deactivated: id={}", id);
    }

    private NotificationTemplate getEntityById(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException(id.toString()));
    }

    private TemplateResponse toResponse(NotificationTemplate t) {
        return TemplateResponse.builder()
                .id(t.getId()).name(t.getName()).description(t.getDescription())
                .channelType(t.getChannelType()).eventType(t.getEventType())
                .subjectTemplate(t.getSubjectTemplate()).bodyTemplate(t.getBodyTemplate())
                .htmlBodyTemplate(t.getHtmlBodyTemplate()).availableVariables(t.getAvailableVariables())
                .locale(t.getLocale()).version(t.getVersion()).active(t.isActive())
                .createdBy(t.getCreatedBy()).createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .build();
    }
}
