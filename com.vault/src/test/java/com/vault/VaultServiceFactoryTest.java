package com.vault;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VaultServiceFactoryTest {
    @Test
    void usesFallbackWhenVaultIsNotConfigured() {
        VaultService service = VaultServiceFactory.create(
                Map.of(
                        "vault.authorization.client-id", "sample.app",
                        "vault.authorization.allowed-keys", "APP_SECRET"),
                Map.of("APP_SECRET", "from-env"),
                Map.of("APP_SECRET", "from-config"),
                "sample.app");

        assertThat(service.requireSecret("APP_SECRET")).isEqualTo("from-env");
    }

    @Test
    void deniesFallbackSecretWhenKeyIsNotAuthorized() {
        VaultService service = VaultServiceFactory.create(
                Map.of(
                        "vault.authorization.client-id", "sample.app",
                        "vault.authorization.allowed-keys", "APP_SECRET"),
                Map.of("OTHER_SECRET", "from-env"),
                Map.of(),
                "sample.app");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.requireSecret("OTHER_SECRET"))
                .isInstanceOf(SecurityException.class);
    }
}
