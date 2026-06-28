package com.notificationengine.exception;

/** Thrown when a GDPR/compliance rule is violated. */
public class ComplianceViolationException extends NotificationException {
    public ComplianceViolationException(String userId, String reason) {
        super(ErrorCode.COMPLIANCE_VIOLATION,
              "Compliance violation for user " + userId + ": " + reason, 403);
    }
}
