package eu.egm.data.common;

import java.time.Instant;

public record WorkflowTaskView(
        String taskId,
        String name,
        WorkflowTaskStatus status,
        Instant startedAt,
        Instant completedAt,
        String message) {
}
