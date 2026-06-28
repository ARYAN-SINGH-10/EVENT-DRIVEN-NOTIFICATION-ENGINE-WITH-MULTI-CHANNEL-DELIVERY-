package com.notificationengine.service.impl;

import com.notificationengine.dto.ChannelRequest;
import com.notificationengine.dto.ChannelResponse;
import com.notificationengine.entity.NotificationChannel;
import com.notificationengine.exception.ChannelUnavailableException;
import com.notificationengine.exception.ErrorCode;
import com.notificationengine.exception.NotificationException;
import com.notificationengine.repository.NotificationChannelRepository;
import com.notificationengine.service.ChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final NotificationChannelRepository channelRepository;

    @Override
    @Transactional
    @CacheEvict(value = "channels", allEntries = true)
    public ChannelResponse create(ChannelRequest request) {
        if (channelRepository.existsByChannelType(request.getChannelType())) {
            throw new NotificationException(ErrorCode.CHANNEL_ALREADY_EXISTS,
                    "Channel already exists: " + request.getChannelType(), 409);
        }
        NotificationChannel channel = NotificationChannel.builder()
                .channelType(request.getChannelType())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .active(request.isActive())
                .simulationMode(request.isSimulationMode())
                .providerConfig(request.getProviderConfig())
                .rateLimitPerMinute(request.getRateLimitPerMinute())
                .supportsHtml(request.isSupportsHtml())
                .supportsAttachments(request.isSupportsAttachments())
                .build();
        return toResponse(channelRepository.save(channel));
    }

    @Override
    @Transactional
    @CacheEvict(value = "channels", allEntries = true)
    public ChannelResponse update(UUID id, ChannelRequest request) {
        NotificationChannel channel = getEntityById(id);
        channel.setDisplayName(request.getDisplayName());
        channel.setDescription(request.getDescription());
        channel.setActive(request.isActive());
        channel.setSimulationMode(request.isSimulationMode());
        channel.setProviderConfig(request.getProviderConfig());
        channel.setRateLimitPerMinute(request.getRateLimitPerMinute());
        channel.setSupportsHtml(request.isSupportsHtml());
        channel.setSupportsAttachments(request.isSupportsAttachments());
        return toResponse(channelRepository.save(channel));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "channels", key = "#id")
    public ChannelResponse getById(UUID id) {
        return toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "channels", key = "'all'")
    public List<ChannelResponse> getAll() {
        return channelRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "channels", key = "'active'")
    public List<ChannelResponse> getActive() {
        return channelRepository.findByActive(true).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "channels", allEntries = true)
    public ChannelResponse toggleActive(UUID id, boolean active) {
        NotificationChannel channel = getEntityById(id);
        channel.setActive(active);
        return toResponse(channelRepository.save(channel));
    }

    @Override
    @Transactional
    @CacheEvict(value = "channels", allEntries = true)
    public void delete(UUID id) {
        if (!channelRepository.existsById(id)) {
            throw new NotificationException(ErrorCode.CHANNEL_NOT_FOUND, "Channel not found: " + id, 404);
        }
        channelRepository.deleteById(id);
    }

    private NotificationChannel getEntityById(UUID id) {
        return channelRepository.findById(id)
                .orElseThrow(() -> new NotificationException(
                        ErrorCode.CHANNEL_NOT_FOUND, "Channel not found: " + id, 404));
    }

    private ChannelResponse toResponse(NotificationChannel c) {
        return ChannelResponse.builder()
                .id(c.getId()).channelType(c.getChannelType()).displayName(c.getDisplayName())
                .description(c.getDescription()).active(c.isActive())
                .simulationMode(c.isSimulationMode()).rateLimitPerMinute(c.getRateLimitPerMinute())
                .maxPayloadSizeBytes(c.getMaxPayloadSizeBytes()).supportsHtml(c.isSupportsHtml())
                .supportsAttachments(c.isSupportsAttachments())
                .createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt())
                .build();
    }
}
