package com.env;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves the deployment environment used by the suite configuration loader.
 * The canonical key is {@code env}; environment variables can use
 * {@code ENV}. Blank or missing values deliberately resolve to local.
 */
public final class EnvironmentResolverService {

    public static final String DEFAULT_ENVIRONMENT = "local";
    public static final String SYSTEM_PROPERTY = "env";
    public static final String ENVIRONMENT_VARIABLE = "ENV";

    private EnvironmentResolverService() {
    }

    public static String resolve() {
        return resolve(System.getProperties().stringPropertyNames().stream()
                .filter(SYSTEM_PROPERTY::equals)
                .findFirst()
                .map(System::getProperty)
                .orElse(null), System.getenv());
    }

    public static String resolve(String systemPropertyValue, Map<String, String> environment) {
        return normalize(Optional.ofNullable(systemPropertyValue)
                .filter(value -> !value.isBlank())
                .orElseGet(() -> environment.get(ENVIRONMENT_VARIABLE)));
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_ENVIRONMENT;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
