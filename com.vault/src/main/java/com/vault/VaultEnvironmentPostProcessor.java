package com.vault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

public class VaultEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String MODULE_PROPERTY = "module";
    private static final String MODULE_ENV = "MODULE";
    private static final String PROPERTY_SOURCE_NAME = "vault-resolved";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = extractProperties(environment);
        VaultService vaultService = VaultServiceFactory.create(
                properties,
                System.getenv(),
                properties,
                clientId(environment));
        Map<String, Object> resolved = new VaultPlaceholderResolver(vaultService).resolve(properties);
        if (!resolved.equals(properties)) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, resolved));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private Map<String, Object> extractProperties(ConfigurableEnvironment environment) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource) {
                for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                    properties.putIfAbsent(propertyName, enumerablePropertySource.getProperty(propertyName));
                }
            }
        }
        return properties;
    }

    private String clientId(ConfigurableEnvironment environment) {
        String property = environment.getProperty(MODULE_PROPERTY);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        String systemProperty = System.getProperty(MODULE_PROPERTY);
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty.trim();
        }
        String envValue = System.getenv(MODULE_ENV);
        return envValue == null || envValue.isBlank() ? null : envValue.trim();
    }
}
