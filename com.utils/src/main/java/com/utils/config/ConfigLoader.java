package com.utils.config;

import com.utils.cache.CacheConfigurationService;
import com.utils.cache.CacheService;
import com.utils.cache.CacheServiceFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class ConfigLoader {

    private final CacheService<String, LoadedConfiguration> cache;

    public ConfigLoader(CacheConfigurationService cacheConfiguration) {
        this.cache = CacheServiceFactory.create(cacheConfiguration);
    }

    public LoadedConfiguration load(ConfigResourceName resourceName) {
        return cache.getOrLoad(resourceName.classpathLocation(), () -> loadUncached(resourceName));
    }

    private LoadedConfiguration loadUncached(ConfigResourceName resourceName) {
        String location = resourceName.classpathLocation();
        Resource resource = new ClassPathResource(location);
        if (!resource.exists()) {
            return new LoadedConfiguration(location, Map.of());
        }
        try {
            Map<String, Object> properties = switch (resourceName.extension()) {
                case "xml" -> loadXml(resource);
                case "yml", "yaml" -> loadYaml(location, resource);
                default -> throw new IllegalArgumentException("Unsupported config extension: " + resourceName.extension());
            };
            return new LoadedConfiguration(location, properties);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load configuration resource " + location, ex);
        }
    }

    private Map<String, Object> loadXml(Resource resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = resource.getInputStream()) {
            properties.loadFromXML(input);
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name));
        }
        return values;
    }

    private Map<String, Object> loadYaml(String name, Resource resource) throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        Map<String, Object> values = new LinkedHashMap<>();
        for (PropertySource<?> source : loader.load(name, resource)) {
            if (source.getSource() instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key) {
                        values.put(key, entry.getValue());
                    }
                }
            }
        }
        return values;
    }
}
