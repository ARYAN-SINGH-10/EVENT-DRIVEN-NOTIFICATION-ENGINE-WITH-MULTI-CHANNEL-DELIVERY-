package com.notificationengine.util;

import com.notificationengine.entity.NotificationEntity;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * Notification-domain utility methods.
 */
public final class NotificationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[1-9]\\d{6,14}$");

    private NotificationUtil() {}

    /**
     * Validates the recipient address for the given channel type.
     */
    public static boolean isValidRecipient(String address, ChannelType channelType) {
        if (StringUtils.isBlank(address)) return false;
        return switch (channelType) {
            case EMAIL    -> EMAIL_PATTERN.matcher(address).matches();
            case SMS, WHATSAPP, CALL -> PHONE_PATTERN.matcher(address).matches();
            case PUSH     -> address.length() >= 10; // device token
            case SLACK    -> address.startsWith("U") || address.startsWith("C"); // Slack user/channel ID
            case WEBHOOK  -> address.startsWith("http://") || address.startsWith("https://");
            case IN_APP   -> true; // In-app just uses userId which is already non-blank
        };
    }

    /**
     * Masks a recipient address for logging (privacy).
     */
    public static String maskRecipient(String address, ChannelType channelType) {
        if (StringUtils.isBlank(address)) return "***";
        return switch (channelType) {
            case EMAIL -> {
                int atIndex = address.indexOf('@');
                if (atIndex <= 0) yield "***";
                String local = address.substring(0, atIndex);
                String domain = address.substring(atIndex);
                yield (local.length() > 2 ? local.charAt(0) + "***" : "***") + domain;
            }
            case SMS -> {
                if (address.length() < 4) yield "***";
                yield "***" + address.substring(address.length() - 4);
            }
            default -> address.substring(0, Math.min(4, address.length())) + "***";
        };
    }

    /**
     * Returns true if the notification status is terminal (cannot be retried).
     */
    public static boolean isTerminalStatus(NotificationEntity.NotificationStatus status) {
        return status == NotificationEntity.NotificationStatus.DELIVERED
                || status == NotificationEntity.NotificationStatus.CANCELLED
                || status == NotificationEntity.NotificationStatus.EXPIRED;
    }

    /**
     * Truncates a string to the given max length, appending "..." if truncated.
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) return null;
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }
}
