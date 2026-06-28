package com.notificationengine.exception;

import lombok.Getter;

/**
 * Base runtime exception for all notification engine errors.
 */
@Getter
public class NotificationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final int httpStatus;

    public NotificationException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.httpStatus = 500;
    }

    public NotificationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 500;
    }

    public NotificationException(ErrorCode errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public NotificationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = 500;
    }
}
