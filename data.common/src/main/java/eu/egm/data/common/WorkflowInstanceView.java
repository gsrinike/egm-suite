package eu.egm.data.common;

import java.time.Instant;
import java.util.List;

public record WorkflowInstanceView(
        String processInstanceId,
        String processId,
        String businessKey,
        WorkflowStatus status,
        List<WorkflowTaskView> tasks,
        Instant startedAt,
        Instant endedAt) {
    public WorkflowInstanceView {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }
}
