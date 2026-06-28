package com.notificationengine.template;

import lombok.Getter;

/**
 * Supported locales for template localization.
 */
@Getter
public enum TemplateLocale {

    EN("en", "English"),
    HI("hi", "Hindi"),
    MR("mr", "Marathi"),
    TA("ta", "Tamil"),
    TE("te", "Telugu");

    private final String code;
    private final String displayName;

    TemplateLocale(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public static TemplateLocale fromCode(String code) {
        if (code == null) return EN;
        for (TemplateLocale locale : values()) {
            if (locale.code.equalsIgnoreCase(code) || locale.name().equalsIgnoreCase(code)) {
                return locale;
            }
        }
        return EN; // Default fallback
    }
}
