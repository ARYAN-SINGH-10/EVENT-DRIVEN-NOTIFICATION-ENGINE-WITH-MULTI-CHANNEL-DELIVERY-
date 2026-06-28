package com.notificationengine.exception;

import lombok.Getter;

/**
 * Enumeration of application-level error codes.
 */
@Getter
public enum ErrorCode {

    // General
    INTERNAL_SERVER_ERROR("NE-001", "Internal server error"),
    VALIDATION_ERROR("NE-002", "Validation failed"),
    RESOURCE_NOT_FOUND("NE-003", "Resource not found"),
    DUPLICATE_RESOURCE("NE-004", "Resource already exists"),
    BAD_REQUEST("NE-005", "Bad request"),

    // Notification
    NOTIFICATION_NOT_FOUND("NE-100", "Notification not found"),
    NOTIFICATION_SEND_FAILED("NE-101", "Notification delivery failed"),
    NOTIFICATION_ALREADY_SENT("NE-102", "Notification already sent"),
    NOTIFICATION_EXPIRED("NE-103", "Notification has expired"),
    NOTIFICATION_CANCELLED("NE-104", "Notification has been cancelled"),

    // Template
    TEMPLATE_NOT_FOUND("NE-200", "Template not found"),
    TEMPLATE_ALREADY_EXISTS("NE-201", "Template with this name already exists"),
    TEMPLATE_RENDER_FAILED("NE-202", "Template rendering failed"),
    TEMPLATE_INACTIVE("NE-203", "Template is inactive"),
    MISSING_TEMPLATE_VARIABLE("NE-204", "Required template variable is missing"),

    // Channel
    CHANNEL_NOT_FOUND("NE-300", "Channel not found"),
    CHANNEL_UNAVAILABLE("NE-301", "Channel is unavailable"),
    CHANNEL_RATE_LIMIT_EXCEEDED("NE-302", "Channel rate limit exceeded"),
    CHANNEL_ALREADY_EXISTS("NE-303", "Channel already configured"),

    // User Preference
    PREFERENCE_NOT_FOUND("NE-400", "User preference not found"),
    PREFERENCE_ALREADY_EXISTS("NE-401", "User preference already exists for this channel"),
    USER_OPT_OUT("NE-402", "User has opted out of this notification type"),

    // DLQ
    DLQ_ENTRY_NOT_FOUND("NE-500", "DLQ entry not found"),
    DLQ_MAX_RETRIES_EXCEEDED("NE-501", "Maximum retry attempts exceeded"),
    DLQ_ALREADY_RESOLVED("NE-502", "DLQ entry already resolved"),

    // Compliance
    COMPLIANCE_VIOLATION("NE-600", "Compliance violation detected"),
    CONSENT_NOT_GRANTED("NE-601", "User has not granted required consent"),
    DATA_RETENTION_VIOLATION("NE-602", "Data retention policy violation"),
    GDPR_DELETION_REQUIRED("NE-603", "GDPR deletion has been requested for this user"),

    // Kafka / Events
    EVENT_PUBLISH_FAILED("NE-700", "Event publishing failed"),
    EVENT_NOT_FOUND("NE-701", "Event not found");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
