package com.vault;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentVaultServiceTest {
    @Test
    void prefersEnvironmentValueOverConfigValue() {
        EnvironmentVaultService service = new EnvironmentVaultService(
                Map.of("MINIO_SECRET_KEY", "from-env"),
                Map.of("MINIO_SECRET_KEY", "from-config"));

        assertThat(service.requireSecret("MINIO_SECRET_KEY")).isEqualTo("from-env");
    }

    @Test
    void fallsBackToConfigurationValue() {
        EnvironmentVaultService service = new EnvironmentVaultService(
                Map.of(),
                Map.of("MINIO_SECRET_KEY", "from-config"));

        assertThat(service.requireSecret("MINIO_SECRET_KEY")).isEqualTo("from-config");
    }
}
