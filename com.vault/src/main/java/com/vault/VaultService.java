package com.vault;

import java.util.Optional;

public interface VaultService {
    Optional<String> getSecret(String key);

    default String requireSecret(String key) {
        return getSecret(key)
                .orElseThrow(() -> new IllegalStateException("Secret is not available: " + key));
    }
}
