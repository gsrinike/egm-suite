package com.utils.cache;

import com.utils.cache.jdk.JavaCacheService;
import java.util.Locale;

public final class CacheServiceFactory {

    private CacheServiceFactory() {
    }

    public static <K, V> CacheService<K, V> create(CacheConfigurationService configuration) {
        String provider = configuration.provider().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "java" -> new JavaCacheService<>(configuration);
            case "none" -> new JavaCacheService<>(new CacheConfigurationService("none", false, 0, configuration.ttl()));
            default -> throw new IllegalArgumentException("Unsupported cache provider: " + configuration.provider());
        };
    }
}
