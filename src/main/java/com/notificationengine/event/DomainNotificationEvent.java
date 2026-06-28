package com.notificationengine.event;

import com.notificationengine.entity.NotificationEvent;
import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent wrapping a persisted {@link NotificationEvent}.
 */
public class DomainNotificationEvent extends ApplicationEvent {

    private final NotificationEvent notificationEvent;

    public DomainNotificationEvent(Object source, NotificationEvent notificationEvent) {
        super(source);
        this.notificationEvent = notificationEvent;
    }

    public NotificationEvent getNotificationEvent() {
        return notificationEvent;
    }
}
