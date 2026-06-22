package com.vault;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VaultPlaceholderResolverTest {
    @Test
    void resolvesVaultPlaceholderInsideConfigValue() {
        VaultPlaceholderResolver resolver = new VaultPlaceholderResolver(
                VaultServiceFactory.create(
                        Map.of(
                                "vault.authorization.client-id", "srv.cgm.importer",
                                "vault.authorization.allowed-keys", "MINIO_SECRET_KEY"),
                        Map.of("MINIO_SECRET_KEY", "secret-value"),
                        Map.of(),
                        "srv.cgm.importer"));

        Map<String, Object> resolved = resolver.resolve(Map.of(
                "utility.object-storage.access-key", "${vault:MINIO_SECRET_KEY}",
                "utility.object-storage.endpoint", "http://localhost:9000"));

        assertThat(resolved.get("utility.object-storage.access-key")).isEqualTo("secret-value");
        assertThat(resolved.get("utility.object-storage.endpoint")).isEqualTo("http://localhost:9000");
    }
}
