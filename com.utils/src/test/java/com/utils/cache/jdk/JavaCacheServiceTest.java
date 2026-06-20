package com.utils.cache.jdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.utils.cache.CacheConfigurationService;
import com.utils.cache.CacheService;
import com.utils.cache.CacheServiceFactory;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JavaCacheServiceTest {

    @Test
    void returnsCachedValueWhenEnabled() {
        JavaCacheService<String, String> cache = new JavaCacheService<>(new CacheConfigurationService("java", true, 10, Duration.ofMinutes(1)));

        String first = cache.getOrLoad("a", () -> "one");
        String second = cache.getOrLoad("a", () -> "two");

        assertThat(first).isEqualTo("one");
        assertThat(second).isEqualTo("one");
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void bypassesCacheWhenDisabled() {
        JavaCacheService<String, String> cache = new JavaCacheService<>(new CacheConfigurationService("java", false, 10, Duration.ofMinutes(1)));

        assertThat(cache.getOrLoad("a", () -> "one")).isEqualTo("one");
        assertThat(cache.getOrLoad("a", () -> "two")).isEqualTo("two");
        assertThat(cache.size()).isZero();
    }

    @Test
    void factoryCanDisableCacheThroughProviderConfiguration() {
        CacheService<String, String> cache = CacheServiceFactory.create(new CacheConfigurationService("NONE", true, 10, Duration.ofMinutes(1)));

        assertThat(cache.getOrLoad("a", () -> "one")).isEqualTo("one");
        assertThat(cache.getOrLoad("a", () -> "two")).isEqualTo("two");
        assertThat(cache.size()).isZero();
    }
}
