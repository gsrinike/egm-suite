package com.utils.profile;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

/**
 * Thread-safe loader for profile conversion defaults stored under
 * {@code config/profile/<family>/<name>.yml}.
 */
public class ProfileDefaultsService {
    private final Map<String, ProfileDefaults> cache = new ConcurrentHashMap<>();

    public ProfileDefaults load(String family, String name) {
        String normalizedFamily = normalize(family, "common");
        String normalizedName = normalize(name, "defaults");
        String key = normalizedFamily + "/" + normalizedName;
        return cache.computeIfAbsent(key, ignored -> loadUncached(normalizedFamily, normalizedName));
    }

    private ProfileDefaults loadUncached(String family, String name) {
        String location = "config/profile/%s/%s.yml".formatted(family, name);
        ClassPathResource resource = new ClassPathResource(location);
        if (!resource.exists()) {
            return new ProfileDefaults(location, Map.of());
        }
        try (InputStream inputStream = resource.getInputStream()) {
            Object loaded = new Yaml().load(inputStream);
            return new ProfileDefaults(location, flatten(loaded));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load profile defaults: " + location, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flatten(Object loaded) {
        if (!(loaded instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        flatten("", (Map<Object, Object>) map, values);
        return Map.copyOf(values);
    }

    private void flatten(String prefix, Map<Object, Object> source, Map<String, Object> target) {
        source.forEach((key, value) -> {
            String path = prefix.isBlank() ? String.valueOf(key) : prefix + "." + key;
            if (value instanceof Map<?, ?> nested) {
                Map<Object, Object> nestedMap = new LinkedHashMap<>();
                nested.forEach(nestedMap::put);
                flatten(path, nestedMap, target);
            } else {
                target.put(path, value);
            }
        });
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toLowerCase();
    }
}
