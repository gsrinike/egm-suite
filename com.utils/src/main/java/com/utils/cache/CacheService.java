package com.utils.cache;

import java.util.Optional;
import java.util.function.Supplier;

public interface CacheService<K, V> {
    Optional<V> get(K key);

    V getOrLoad(K key, Supplier<V> supplier);

    void put(K key, V value);

    int size();
}
