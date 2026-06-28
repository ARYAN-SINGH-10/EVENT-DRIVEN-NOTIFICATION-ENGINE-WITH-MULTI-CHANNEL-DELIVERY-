package com.notificationengine.util;

import java.util.UUID;

/**
 * UUID generation and correlation ID utilities.
 */
public final class UuidUtil {

    private UuidUtil() {}

    /** Generates a random UUID string. */
    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /** Generates a compact UUID without hyphens. */
    public static String generateCompact() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** Generates a correlation ID prefixed with "corr-". */
    public static String generateCorrelationId() {
        return "corr-" + UUID.randomUUID().toString().substring(0, 16);
    }

    /** Returns true if the given string is a valid UUID. */
    public static boolean isValidUUID(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
