package com.utils.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;

class ConfigEnvironmentPostProcessorTest {

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty(ModuleName.SYSTEM_PROPERTY);
        System.clearProperty("env");
        System.clearProperty("SAMPLE_URI");
    }

    @Test
    void resolvesEnvironmentPlaceholdersButLeavesVaultReferencesForVaultPostProcessor() {
        System.setProperty(ModuleName.SYSTEM_PROPERTY, "test.module");
        System.setProperty("env", "local");
        System.setProperty("SAMPLE_URI", "http://example:9200");
        StandardEnvironment environment = new StandardEnvironment();

        new ConfigEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty("sample.uri")).isEqualTo("http://example:9200");
        assertThat(environment.getProperty("sample.default-uri")).isEqualTo("http://default:9200");
        assertThat(environment.getPropertySources()
                .get("local/test.module-infra.yml")
                .getProperty("sample.secret"))
                .isEqualTo("${vault:MINIO_SECRET_KEY}");
    }
}
