package com.utils.cache;

import java.time.Clock;

public abstract class AbstractCacheService<K, V> implements CacheService<K, V> {

    protected final CacheConfigurationService configuration;
    protected final Clock clock;

    protected AbstractCacheService(CacheConfigurationService configuration) {
        this(configuration, Clock.systemUTC());
    }

    protected AbstractCacheService(CacheConfigurationService configuration, Clock clock) {
        this.configuration = configuration;
        this.clock = clock;
    }
}
