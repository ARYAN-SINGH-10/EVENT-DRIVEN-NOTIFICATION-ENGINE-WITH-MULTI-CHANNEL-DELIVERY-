package com.notificationengine.exception;

/** Thrown when a user preference record cannot be found. */
public class UserPreferenceNotFoundException extends NotificationException {
    public UserPreferenceNotFoundException(String userId, String channel) {
        super(ErrorCode.PREFERENCE_NOT_FOUND,
              "User preference not found for userId=" + userId + ", channel=" + channel, 404);
    }
}
