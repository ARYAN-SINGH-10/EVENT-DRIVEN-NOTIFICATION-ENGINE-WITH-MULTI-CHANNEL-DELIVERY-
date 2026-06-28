package com.notificationengine.channel;

import com.notificationengine.entity.NotificationEntity;

/**
 * Strategy interface for channel-specific notification delivery.
 *
 * <p>Each channel (Email, SMS, Push, Slack, Webhook) implements this interface
 * and is discovered by the {@link ChannelProcessorFactory}.</p>
 */
public interface NotificationChannelProcessor {

    /**
     * Returns the channel type this processor handles.
     */
    NotificationEntity.ChannelType getChannelType();

    /**
     * Sends the notification through this channel.
     *
     * @param notification  the notification entity to send
     * @return external message ID if available, or a simulation reference
     */
    String send(NotificationEntity notification);

    /**
     * Returns true if this processor is currently available.
     */
    boolean isAvailable();
}
