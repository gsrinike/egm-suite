package com.utils.config;

import com.utils.cache.CacheConfigurationService;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

final class CacheConfigLoader {

    private CacheConfigLoader() {
    }

    static CacheConfigurationService from(Map<String, Object> properties) {
        boolean enabled = booleanValue(properties.get("config.cache.enabled"), true);
        String provider = stringValue(properties.get("config.cache.provider"), "java").toLowerCase(Locale.ROOT);
        int maximumSize = intValue(properties.get("config.cache.maximum-size"), 256);
        Duration ttl = durationValue(properties.get("config.cache.ttl"), Duration.ofMinutes(15));
        return new CacheConfigurationService(provider, enabled, maximumSize, ttl);
    }

    private static String stringValue(Object value, String defaultValue) {
        return value == null || value.toString().isBlank() ? defaultValue : value.toString();
    }

    private static boolean booleanValue(Object value, boolean defaultValue) {
        return value == null ? defaultValue : Boolean.parseBoolean(value.toString());
    }

    private static int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value.toString());
    }

    private static Duration durationValue(Object value, Duration defaultValue) {
        if (value == null || value.toString().isBlank()) {
            return defaultValue;
        }
        return Duration.parse(value.toString());
    }
}
