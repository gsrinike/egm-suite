package com.infra.bpm;

import java.time.Duration;
import java.util.Map;

public record ProcessMessage(
        String name,
        String correlationKey,
        Map<String, Object> variables,
        Duration timeToLive) {
    public ProcessMessage {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (correlationKey == null || correlationKey.isBlank()) {
            throw new IllegalArgumentException("correlationKey must not be blank");
        }
        variables = variables == null ? Map.of() : Map.copyOf(variables);
        timeToLive = timeToLive == null ? Duration.ofMinutes(5) : timeToLive;
    }
}
