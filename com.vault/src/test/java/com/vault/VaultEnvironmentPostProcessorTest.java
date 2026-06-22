package com.vault;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VaultEnvironmentPostProcessorTest {
    @Test
    void resolvesVaultPlaceholderOnlyWhenApplicationIsAuthorized() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("module", "srv.cgm.importer");
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "vault.authorization.application-id", "srv.cgm.importer",
                "vault.authorization.allowed-keys", "MINIO_SECRET_KEY",
                "MINIO_SECRET_KEY", "secret-value",
                "utility.object-storage.access-key", "${vault:MINIO_SECRET_KEY}")));

        new VaultEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication(Map.class));

        assertThat(environment.getProperty("utility.object-storage.access-key")).isEqualTo("secret-value");
    }
}
