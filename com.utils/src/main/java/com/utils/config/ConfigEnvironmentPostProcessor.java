package com.utils.config;

import com.utils.env.EnvironmentResolverService;
import com.utils.cache.CacheConfigurationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads module-scoped YAML configuration into Spring before bean creation.
 *
 * <p>The post-processor resolves the runtime environment, loads base and
 * environment-specific configuration groups, and resolves ordinary environment
 * placeholders while preserving vault placeholders for the vault processor.</p>
 */
public class ConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?}");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String moduleName = ModuleName.resolve();
        String runtimeEnv = EnvironmentResolverService.resolve(environment.getProperty(EnvironmentResolverService.SYSTEM_PROPERTY), System.getenv());
        environment.getSystemProperties().put(EnvironmentResolverService.SYSTEM_PROPERTY, runtimeEnv);

        ConfigLoader bootstrapLoader = new ConfigLoader(CacheConfigurationService.defaults());
        Map<String, Object> baseCacheProperties = bootstrapLoader.load(new ConfigResourceName(moduleName, "cache-config", "base", "yml")).properties();
        Map<String, Object> envCacheProperties = bootstrapLoader.load(new ConfigResourceName(moduleName, "cache-config", runtimeEnv, "yml")).properties();
        Map<String, Object> cacheProperties = merge(baseCacheProperties, envCacheProperties);

        ConfigLoader loader = new ConfigLoader(CacheConfigLoader.from(cacheProperties));
        addPropertySource(environment, loader, moduleName, "application", runtimeEnv, "yml");
        addPropertySource(environment, loader, moduleName, "infra", runtimeEnv, "yml");
        addYamlPropertySource(environment, "cache-config-" + runtimeEnv, envCacheProperties);
        addPropertySource(environment, loader, moduleName, "vault", runtimeEnv, "yml");
        addPropertySource(environment, loader, moduleName, "application", "base", "yml");
        addPropertySource(environment, loader, moduleName, "infra", "base", "yml");
        addYamlPropertySource(environment, "cache-config-base", baseCacheProperties);
        addPropertySource(environment, loader, moduleName, "vault", "base", "yml");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private void addPropertySource(ConfigurableEnvironment environment, ConfigLoader loader, String moduleName,
                                   String group, String qualifier, String extension) {
        LoadedConfiguration configuration = loader.load(new ConfigResourceName(moduleName, group, qualifier, extension));
        if (!configuration.properties().isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(
                    configuration.name(), resolveEnvironmentPlaceholders(environment, configuration.properties())));
        }
    }

    private void addYamlPropertySource(ConfigurableEnvironment environment, String name, Map<String, Object> properties) {
        if (!properties.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(name, properties));
        }
    }

    private Map<String, Object> merge(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> merged = new LinkedHashMap<>();
        for (Map<String, Object> source : List.of(base, override)) {
            merged.putAll(source);
        }
        return merged;
    }

    private Map<String, Object> resolveEnvironmentPlaceholders(ConfigurableEnvironment environment, Map<String, Object> properties) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object value = unwrapOriginTrackedValue(entry.getValue());
            if (value instanceof String stringValue && !stringValue.contains("${vault:")) {
                resolved.put(entry.getKey(), resolvePlaceholderValue(environment, stringValue));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }

    private Object unwrapOriginTrackedValue(Object value) {
        if (value instanceof OriginTrackedValue originTrackedValue) {
            return originTrackedValue.getValue();
        }
        return value;
    }

    private String resolvePlaceholderValue(ConfigurableEnvironment environment, String value) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String fallback = matcher.group(2);
            String replacement = environment.getProperty(key);
            if (replacement == null) {
                replacement = System.getenv(key);
            }
            if (replacement == null) {
                replacement = fallback;
            }
            if (replacement == null) {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }
}
