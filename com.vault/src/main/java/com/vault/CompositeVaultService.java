package com.vault;

import java.util.List;
import java.util.Optional;

public class CompositeVaultService implements VaultService {
    private final List<VaultService> services;

    public CompositeVaultService(List<VaultService> services) {
        this.services = List.copyOf(services);
    }

    @Override
    public Optional<String> getSecret(String key) {
        for (VaultService service : services) {
            Optional<String> secret = service.getSecret(key);
            if (secret.isPresent()) {
                return secret;
            }
        }
        return Optional.empty();
    }
}
