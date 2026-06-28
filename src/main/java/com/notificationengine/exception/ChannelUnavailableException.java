package com.notificationengine.exception;

/** Thrown when a notification delivery channel is unavailable or misconfigured. */
public class ChannelUnavailableException extends NotificationException {
    public ChannelUnavailableException(String channel) {
        super(ErrorCode.CHANNEL_UNAVAILABLE,
              "Notification channel is unavailable: " + channel, 503);
    }
    public ChannelUnavailableException(String channel, Throwable cause) {
        super(ErrorCode.CHANNEL_UNAVAILABLE,
              "Notification channel is unavailable: " + channel, cause);
    }
}
