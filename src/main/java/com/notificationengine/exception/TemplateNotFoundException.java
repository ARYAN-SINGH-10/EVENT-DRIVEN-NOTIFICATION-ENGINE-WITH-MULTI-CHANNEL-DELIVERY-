package com.notificationengine.exception;

/** Thrown when a notification template cannot be found. */
public class TemplateNotFoundException extends NotificationException {
    public TemplateNotFoundException(String identifier) {
        super(ErrorCode.TEMPLATE_NOT_FOUND,
              "Notification template not found: " + identifier, 404);
    }
}
