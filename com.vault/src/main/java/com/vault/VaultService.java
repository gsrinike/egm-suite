package com.vault;

import java.util.Optional;

/**
 * Secret provider abstraction used by configuration resolution and standalone
 * application code.
 */
public interface VaultService {
    /**
     * Returns the secret value for a logical key when the provider can resolve it.
     *
     * @param key logical secret key, for example {@code MINIO_SECRET_KEY}
     * @return resolved secret value, or an empty result when unavailable
     */
    Optional<String> getSecret(String key);

    /**
     * Resolves a required secret and fails fast when the key cannot be loaded.
     *
     * @param key logical secret key
     * @return resolved secret value
     * @throws IllegalStateException when no provider can supply the key
     */
    default String requireSecret(String key) {
        return getSecret(key)
                .orElseThrow(() -> new IllegalStateException("Secret is not available: " + key));
    }
}
