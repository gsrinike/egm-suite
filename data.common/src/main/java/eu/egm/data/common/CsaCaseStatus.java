package eu.egm.data.common;

import java.time.Instant;
import java.util.List;

public record CsaCaseStatus(
        String csaCaseId,
        String caseName,
        WorkflowStatus status,
        NetworkCaseReference networkCase,
        String processInstanceId,
        LoadFlowResult loadFlowResult,
        SecurityAnalysisResult securityAnalysisResult,
        RaoResult raoResult,
        List<WorkflowTaskView> tasks,
        Instant createdAt,
        Instant updatedAt,
        String message) {
    public CsaCaseStatus {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }
}
