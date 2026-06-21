package com.utils.config;

import com.utils.env.EnvironmentResolverService;
import com.utils.cache.CacheConfigurationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class ConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

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
        addPropertySource(environment, loader, moduleName, "application", runtimeEnv, "xml");
        addPropertySource(environment, loader, moduleName, "infra", runtimeEnv, "xml");
        addYamlPropertySource(environment, "cache-config-" + runtimeEnv, envCacheProperties);
        addPropertySource(environment, loader, moduleName, "application", "base", "xml");
        addPropertySource(environment, loader, moduleName, "infra", "base", "xml");
        addYamlPropertySource(environment, "cache-config-base", baseCacheProperties);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private void addPropertySource(ConfigurableEnvironment environment, ConfigLoader loader, String moduleName,
                                   String group, String qualifier, String extension) {
        LoadedConfiguration configuration = loader.load(new ConfigResourceName(moduleName, group, qualifier, extension));
        if (!configuration.properties().isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(configuration.name(), configuration.properties()));
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
}
