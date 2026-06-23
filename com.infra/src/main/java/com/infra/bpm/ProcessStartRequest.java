package com.infra.bpm;

import java.util.Map;

public record ProcessStartRequest(
        String processId,
        Map<String, Object> variables,
        String businessKey) {
    public ProcessStartRequest {
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("processId must not be blank");
        }
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }
}
