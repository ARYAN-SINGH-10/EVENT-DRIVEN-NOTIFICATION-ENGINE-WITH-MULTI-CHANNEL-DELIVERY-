package com.notificationengine.template;

import com.notificationengine.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves and enriches template variables before rendering.
 *
 * <p>Adds built-in system variables (timestamp, date, year) and merges
 * user-provided variables with event payload data.</p>
 */
@Slf4j
@Component
public class TemplateVariableResolver {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Resolves the complete variable map for a template rendering.
     *
     * @param userVariables      variables from the notification request
     * @param eventPayloadJson   raw JSON payload from the triggering event
     * @return merged variable map
     */
    public Map<String, String> resolve(Map<String, String> userVariables, String eventPayloadJson) {
        Map<String, String> resolved = new HashMap<>();

        // 1. Add system built-ins
        resolved.putAll(buildSystemVariables());

        // 2. Merge event payload if present
        if (StringUtils.isNotBlank(eventPayloadJson)) {
            JsonUtil.toStringMap(eventPayloadJson).ifPresent(resolved::putAll);
        }

        // 3. User-provided variables override everything (highest priority)
        if (userVariables != null) {
            resolved.putAll(userVariables);
        }

        return resolved;
    }

    /**
     * Parses a template variables JSON string into a Map.
     */
    public Map<String, String> parseVariablesJson(String variablesJson) {
        if (StringUtils.isBlank(variablesJson)) return new HashMap<>();
        return JsonUtil.toStringMap(variablesJson).orElse(new HashMap<>());
    }

    private Map<String, String> buildSystemVariables() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> sysVars = new HashMap<>();
        sysVars.put("currentDate", now.format(DATE_FORMATTER));
        sysVars.put("currentDateTime", now.format(DATETIME_FORMATTER));
        sysVars.put("currentYear", String.valueOf(now.getYear()));
        sysVars.put("appName", "Notification Engine");
        return sysVars;
    }
}
