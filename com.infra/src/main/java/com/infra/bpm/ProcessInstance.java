package com.infra.bpm;

import java.time.OffsetDateTime;

public record ProcessInstance(
        String processInstanceId,
        String processId,
        int version,
        ProcessInstanceStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt) {
}
