package com.notificationengine.service;

import com.notificationengine.dto.UserPreferenceRequest;
import com.notificationengine.dto.UserPreferenceResponse;
import com.notificationengine.entity.NotificationEntity.ChannelType;

import java.util.List;
import java.util.UUID;

public interface UserPreferenceService {
    UserPreferenceResponse createOrUpdate(UserPreferenceRequest request);
    UserPreferenceResponse getById(UUID id);
    List<UserPreferenceResponse> getByUserId(String userId);
    UserPreferenceResponse getByUserIdAndChannel(String userId, ChannelType channelType);
    void delete(UUID id);
    void deleteByUserId(String userId);
}
