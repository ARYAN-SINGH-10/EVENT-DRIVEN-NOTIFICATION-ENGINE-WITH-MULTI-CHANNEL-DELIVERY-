package com.notificationengine.service;

import com.notificationengine.entity.NotificationEntity;
import com.notificationengine.entity.NotificationEntity.NotificationStatus;

public interface NotificationStateMachineService {

    /**
     * Transitions a notification to a new state and creates an audit log entry.
     * 
     * @param notification The notification entity
     * @param newState     The target state
     * @param reason       The reason for the transition
     * @return The updated notification entity (already saved)
     */
    NotificationEntity transitionState(NotificationEntity notification, NotificationStatus newState, String reason);
}
