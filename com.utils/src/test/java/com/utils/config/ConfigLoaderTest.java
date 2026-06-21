package com.utils.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.utils.cache.CacheConfigurationService;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConfigLoaderTest {

    @Test
    void loadsXmlPropertiesFromClasspath() {
        ConfigLoader loader = new ConfigLoader(new CacheConfigurationService("java", true, 16, Duration.ofMinutes(1)));

        LoadedConfiguration configuration = loader.load(new ConfigResourceName("test.module", "application", "base", "xml"));

        assertThat(configuration.properties())
                .containsEntry("sample.name", "base")
                .containsEntry("sample.timeout", "PT5S");
    }

    @Test
    void loadsFlattenedYamlPropertiesFromClasspath() {
        ConfigLoader loader = new ConfigLoader(new CacheConfigurationService("java", true, 16, Duration.ofMinutes(1)));

        LoadedConfiguration configuration = loader.load(new ConfigResourceName("test.module", "cache-config", "base", "yml"));

        assertThat(configuration.properties().get("config.cache.enabled").toString()).isEqualTo("true");
        assertThat(configuration.properties().get("config.cache.maximum-size").toString()).isEqualTo("8");
        assertThat(configuration.properties().get("config.cache.ttl").toString()).isEqualTo("PT10S");
    }
}
