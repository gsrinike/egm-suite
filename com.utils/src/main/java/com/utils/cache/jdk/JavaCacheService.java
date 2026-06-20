package com.utils.cache.jdk;

import com.utils.cache.AbstractCacheService;
import com.utils.cache.CacheConfigurationService;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Small in-memory Java cache used for configuration resources. It avoids adding
 * a cache provider dependency while keeping expiry and size behavior explicit.
 */
public class JavaCacheService<K, V> extends AbstractCacheService<K, V> {

    private final Map<K, CacheEntry<V>> entries;

    public JavaCacheService(CacheConfigurationService configuration) {
        this(configuration, Clock.systemUTC());
    }

    public JavaCacheService(CacheConfigurationService configuration, Clock clock) {
        super(configuration, clock);
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return size() > JavaCacheService.this.configuration.maximumSize();
            }
        };
    }

    @Override
    public synchronized Optional<V> get(K key) {
        if (!configuration.enabled()) {
            return Optional.empty();
        }
        CacheEntry<V> entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expired(Instant.now(clock))) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public synchronized V getOrLoad(K key, Supplier<V> supplier) {
        return get(key).orElseGet(() -> {
            V value = supplier.get();
            put(key, value);
            return value;
        });
    }

    @Override
    public synchronized void put(K key, V value) {
        if (!configuration.enabled()) {
            return;
        }
        Instant expiresAt = configuration.ttl().isZero() || configuration.ttl().isNegative()
                ? null
                : Instant.now(clock).plus(configuration.ttl());
        entries.put(key, new CacheEntry<>(value, expiresAt));
    }

    @Override
    public synchronized int size() {
        return entries.size();
    }
}
