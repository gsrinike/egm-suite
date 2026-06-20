package com.utils.cache;

import java.time.Duration;

public record CacheConfigurationService(String provider, boolean enabled, int maximumSize, Duration ttl) {

    public static CacheConfigurationService defaults() {
        return new CacheConfigurationService("java", true, 256, Duration.ofMinutes(15));
    }
}
