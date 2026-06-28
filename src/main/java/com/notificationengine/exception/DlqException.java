package com.notificationengine.exception;

/** Thrown when a DLQ operation fails or entry cannot be found. */
public class DlqException extends NotificationException {
    public DlqException(String message) {
        super(ErrorCode.DLQ_ENTRY_NOT_FOUND, message, 404);
    }
    public DlqException(ErrorCode errorCode, String message) {
        super(errorCode, message, 400);
    }
}
