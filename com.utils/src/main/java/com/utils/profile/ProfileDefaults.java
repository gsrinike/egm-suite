package com.utils.profile;

import java.util.List;
import java.util.Map;

/**
 * Immutable flattened profile-default configuration.
 */
public record ProfileDefaults(
        String source,
        Map<String, Object> values) {
    public ProfileDefaults {
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public String stringValue(String key, String fallback) {
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public double doubleValue(String key, double fallback) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    public List<String> stringList(String key) {
        Object value = values.get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return List.of(stringValue);
        }
        return List.of();
    }
}
