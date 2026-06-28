package com.notificationengine.service.impl;

import com.notificationengine.dto.UserPreferenceRequest;
import com.notificationengine.dto.UserPreferenceResponse;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.UserPreference;
import com.notificationengine.exception.ErrorCode;
import com.notificationengine.exception.NotificationException;
import com.notificationengine.exception.UserPreferenceNotFoundException;
import com.notificationengine.repository.UserPreferenceRepository;
import com.notificationengine.service.UserPreferenceService;
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
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceRepository preferenceRepository;

    @Override
    @Transactional
    @CacheEvict(value = "preferences", key = "#request.userId + '-' + #request.channelType")
    public UserPreferenceResponse createOrUpdate(UserPreferenceRequest request) {
        UserPreference pref = preferenceRepository
                .findByUserIdAndChannelType(request.getUserId(), request.getChannelType())
                .orElseGet(() -> UserPreference.builder()
                        .userId(request.getUserId())
                        .channelType(request.getChannelType())
                        .build());

        pref.setEnabled(request.isEnabled());
        pref.setOptedOutEventTypes(request.getOptedOutEventTypes());
        pref.setFrequency(request.getFrequency());
        pref.setQuietHoursStart(request.getQuietHoursStart());
        pref.setQuietHoursEnd(request.getQuietHoursEnd());
        pref.setTimezone(request.getTimezone());
        pref.setLocale(request.getLocale());
        pref.setPriorityOverride(request.getPriorityOverride());

        return toResponse(preferenceRepository.save(pref));
    }

    @Override
    @Transactional(readOnly = true)
    public UserPreferenceResponse getById(UUID id) {
        return preferenceRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotificationException(
                        ErrorCode.PREFERENCE_NOT_FOUND, "Preference not found: " + id, 404));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPreferenceResponse> getByUserId(String userId) {
        return preferenceRepository.findByUserId(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "preferences", key = "#userId + '-' + #channelType")
    public UserPreferenceResponse getByUserIdAndChannel(String userId, ChannelType channelType) {
        return preferenceRepository.findByUserIdAndChannelType(userId, channelType)
                .map(this::toResponse)
                .orElseThrow(() -> new UserPreferenceNotFoundException(userId, channelType.name()));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!preferenceRepository.existsById(id)) {
            throw new NotificationException(ErrorCode.PREFERENCE_NOT_FOUND, "Preference not found: " + id, 404);
        }
        preferenceRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByUserId(String userId) {
        preferenceRepository.deleteByUserId(userId);
        log.info("Deleted all preferences for userId={}", userId);
    }

    private UserPreferenceResponse toResponse(UserPreference p) {
        return UserPreferenceResponse.builder()
                .id(p.getId()).userId(p.getUserId()).channelType(p.getChannelType())
                .enabled(p.isEnabled()).optedOutEventTypes(p.getOptedOutEventTypes())
                .frequency(p.getFrequency()).quietHoursStart(p.getQuietHoursStart())
                .quietHoursEnd(p.getQuietHoursEnd()).timezone(p.getTimezone())
                .locale(p.getLocale()).priorityOverride(p.getPriorityOverride())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }
}
