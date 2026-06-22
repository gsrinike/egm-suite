package com.vault;

import java.util.Map;
import java.util.Optional;

public class EnvironmentVaultService implements VaultService {
    private final Map<String, String> environment;
    private final Map<String, Object> configuration;

    public EnvironmentVaultService(Map<String, String> environment, Map<String, Object> configuration) {
        this.environment = Map.copyOf(environment);
        this.configuration = Map.copyOf(configuration);
    }

    @Override
    public Optional<String> getSecret(String key) {
        String envValue = environment.get(key);
        if (hasText(envValue)) {
            return Optional.of(envValue);
        }
        Object configuredValue = configuration.get(key);
        if (configuredValue instanceof String text && hasText(text)) {
            return Optional.of(text);
        }
        if (configuredValue != null) {
            return Optional.of(configuredValue.toString());
        }
        return Optional.empty();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
