package com.utils.env;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EnvironmentResolverServiceTest {

    @Test
    void defaultsToLocalWhenNoEnvironmentIsSet() {
        assertThat(EnvironmentResolverService.resolve(null, Map.of())).isEqualTo("local");
    }

    @Test
    void systemPropertyWinsOverEnvironmentVariable() {
        assertThat(EnvironmentResolverService.resolve("DEV", Map.of("ENV", "prod"))).isEqualTo("dev");
    }

    @Test
    void readsEnvironmentVariableWhenSystemPropertyIsBlank() {
        assertThat(EnvironmentResolverService.resolve(" ", Map.of("ENV", "TEST"))).isEqualTo("test");
    }
}
