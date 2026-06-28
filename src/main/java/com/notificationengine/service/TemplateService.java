package com.notificationengine.service;

import com.notificationengine.dto.TemplateRequest;
import com.notificationengine.dto.TemplateResponse;
import com.notificationengine.dto.PagedResponse;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TemplateService {
    TemplateResponse create(TemplateRequest request);
    TemplateResponse update(UUID id, TemplateRequest request);
    TemplateResponse getById(UUID id);
    TemplateResponse getByName(String name);
    PagedResponse<TemplateResponse> getAll(Pageable pageable);
    PagedResponse<TemplateResponse> getByChannel(ChannelType channelType, Pageable pageable);
    void delete(UUID id);
    void deactivate(UUID id);
}
