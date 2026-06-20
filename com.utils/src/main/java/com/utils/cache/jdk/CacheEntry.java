package com.utils.cache.jdk;

import java.time.Instant;

record CacheEntry<V>(V value, Instant expiresAt) {

    boolean expired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}
