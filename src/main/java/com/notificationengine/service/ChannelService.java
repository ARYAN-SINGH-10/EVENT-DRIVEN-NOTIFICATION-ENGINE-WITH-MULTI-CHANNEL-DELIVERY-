package com.notificationengine.service;

import com.notificationengine.dto.ChannelRequest;
import com.notificationengine.dto.ChannelResponse;

import java.util.List;
import java.util.UUID;

public interface ChannelService {
    ChannelResponse create(ChannelRequest request);
    ChannelResponse update(UUID id, ChannelRequest request);
    ChannelResponse getById(UUID id);
    List<ChannelResponse> getAll();
    List<ChannelResponse> getActive();
    ChannelResponse toggleActive(UUID id, boolean active);
    void delete(UUID id);
}
