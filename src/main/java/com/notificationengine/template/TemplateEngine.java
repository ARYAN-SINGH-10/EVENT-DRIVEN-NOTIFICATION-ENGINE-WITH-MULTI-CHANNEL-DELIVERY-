package com.notificationengine.template;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handlebars-style template engine supporting:
 * <ul>
 *   <li>{@code {{variableName}}} — simple variable substitution</li>
 *   <li>{@code {{#if condition}}...{{/if}}} — conditional blocks (truthy when variable is non-blank)</li>
 *   <li>{@code {{#each listVar}}...{{/each}}} — iteration over comma-separated values</li>
 *   <li>{@code {{#unless condition}}...{{/unless}}} — inverse conditional</li>
 * </ul>
 */
@Slf4j
@Component
public class TemplateEngine {

    // Matches {{variableName}} (simple substitution)
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    // Matches {{#if varName}}...{{/if}} — including newlines (DOTALL)
    private static final Pattern IF_PATTERN =
            Pattern.compile("\\{\\{#if\\s+(\\w+)\\s*\\}\\}(.*?)\\{\\{/if\\}\\}", Pattern.DOTALL);

    // Matches {{#unless varName}}...{{/unless}}
    private static final Pattern UNLESS_PATTERN =
            Pattern.compile("\\{\\{#unless\\s+(\\w+)\\s*\\}\\}(.*?)\\{\\{/unless\\}\\}", Pattern.DOTALL);

    // Matches {{#each listVar}}...{{/each}} — {{this}} inside refers to the current item
    private static final Pattern EACH_PATTERN =
            Pattern.compile("\\{\\{#each\\s+(\\w+)\\s*\\}\\}(.*?)\\{\\{/each\\}\\}", Pattern.DOTALL);

    // Inside {{#each}}: {{this}} refers to the current item, {{@index}} to the 0-based index
    private static final Pattern THIS_PATTERN = Pattern.compile("\\{\\{this\\}\\}");
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\{\\{@index\\}\\}");

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Renders the given template string by applying all Handlebars-style directives
     * and variable substitutions.
     *
     * @param template  template string with placeholders
     * @param variables variable name → value map
     * @return rendered string
     */
    public String render(String template, Map<String, String> variables) {
        if (template == null) return null;
        if (variables == null) variables = Collections.emptyMap();

        String result = template;

        // 1. Process {{#if}} / {{/if}} blocks
        result = processIf(result, variables);

        // 2. Process {{#unless}} / {{/unless}} blocks
        result = processUnless(result, variables);

        // 3. Process {{#each}} / {{/each}} blocks
        result = processEach(result, variables);

        // 4. Substitute simple {{variable}} placeholders
        result = substituteVariables(result, variables);

        return result;
    }

    /**
     * Validates that all required placeholders in the template have corresponding variable values.
     *
     * @param template  template string
     * @param variables provided variables
     * @return list of missing variable names (empty = all provided)
     */
    public List<String> validateVariables(String template, Map<String, String> variables) {
        List<String> missing = new ArrayList<>();
        if (template == null) return missing;

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        Set<String> checked = new HashSet<>();
        while (matcher.find()) {
            String name = matcher.group(1);
            // Skip Handlebars block keywords
            if (isKeyword(name) || checked.contains(name)) continue;
            checked.add(name);
            if (variables == null || !variables.containsKey(name) || variables.get(name).isBlank()) {
                missing.add(name);
            }
        }
        return missing;
    }

    /**
     * Extracts all variable names defined in a template (excluding block keywords).
     */
    public Set<String> extractVariableNames(String template) {
        Set<String> names = new LinkedHashSet<>();
        if (template == null) return names;

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!isKeyword(name)) {
                names.add(name);
            }
        }
        return names;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private String processIf(String template, Map<String, String> variables) {
        Matcher m = IF_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String conditionVar = m.group(1);
            String block = m.group(2);
            String value = variables.getOrDefault(conditionVar, "");
            boolean truthy = isTruthy(value);
            m.appendReplacement(sb, Matcher.quoteReplacement(truthy ? block : ""));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String processUnless(String template, Map<String, String> variables) {
        Matcher m = UNLESS_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String conditionVar = m.group(1);
            String block = m.group(2);
            String value = variables.getOrDefault(conditionVar, "");
            boolean truthy = isTruthy(value);
            m.appendReplacement(sb, Matcher.quoteReplacement(truthy ? "" : block));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String processEach(String template, Map<String, String> variables) {
        Matcher m = EACH_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String listVar = m.group(1);
            String itemTemplate = m.group(2);
            String rawList = variables.getOrDefault(listVar, "");

            StringBuilder rendered = new StringBuilder();
            if (!rawList.isBlank()) {
                String[] items = rawList.split(",");
                for (int i = 0; i < items.length; i++) {
                    String item = items[i].trim();
                    String block = THIS_PATTERN.matcher(itemTemplate).replaceAll(Matcher.quoteReplacement(item));
                    block = INDEX_PATTERN.matcher(block).replaceAll(String.valueOf(i));
                    rendered.append(block);
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rendered.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String substituteVariables(String template, Map<String, String> variables) {
        Matcher m = VARIABLE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            String value = variables.getOrDefault(name, "");
            if (value == null) value = "";
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private boolean isTruthy(String value) {
        if (value == null || value.isBlank()) return false;
        return !"false".equalsIgnoreCase(value.trim()) && !"0".equals(value.trim());
    }

    private boolean isKeyword(String name) {
        return Set.of("if", "unless", "each", "this", "else").contains(name);
    }
}
