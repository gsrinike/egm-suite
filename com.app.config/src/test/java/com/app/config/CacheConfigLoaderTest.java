package com.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CacheConfigLoaderTest {

    @Test
    void readsCacheConfigurationFromFlattenedProperties() {
        var config = CacheConfigLoader.from(Map.of(
                "config.cache.enabled", "false",
                "config.cache.provider", "JAVA",
                "config.cache.maximum-size", "64",
                "config.cache.ttl", "PT30S"));

        assertThat(config.provider()).isEqualTo("java");
        assertThat(config.enabled()).isFalse();
        assertThat(config.maximumSize()).isEqualTo(64);
        assertThat(config.ttl()).isEqualTo(Duration.ofSeconds(30));
    }
}
