package com.notificationengine.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

/**
 * JSON serialization/deserialization utility methods.
 */
@Slf4j
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtil() {}

    public static String toJson(Object object) {
        if (object == null) return null;
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }

    public static String toJsonPretty(Object object) {
        if (object == null) return null;
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }

    public static <T> Optional<T> fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return Optional.empty();
        try {
            return Optional.of(MAPPER.readValue(json, clazz));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to {}: {}", clazz.getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<Map<String, Object>> toMap(String json) {
        if (json == null || json.isBlank()) return Optional.empty();
        try {
            return Optional.of(MAPPER.readValue(json, new TypeReference<>() {}));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to Map: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<Map<String, String>> toStringMap(String json) {
        if (json == null || json.isBlank()) return Optional.empty();
        try {
            return Optional.of(MAPPER.readValue(json, new TypeReference<>() {}));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to Map<String,String>: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
