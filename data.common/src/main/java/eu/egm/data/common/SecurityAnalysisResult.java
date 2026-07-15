package eu.egm.data.common;

import java.time.Instant;
import java.util.List;

public record SecurityAnalysisResult(
        String resultId,
        WorkflowStatus status,
        List<ContingencyViolation> preContingencyViolations,
        List<ContingencyViolation> postContingencyViolations,
        Instant calculatedAt,
        String message) {
    public SecurityAnalysisResult {
        preContingencyViolations = preContingencyViolations == null ? List.of() : List.copyOf(preContingencyViolations);
        postContingencyViolations = postContingencyViolations == null ? List.of() : List.copyOf(postContingencyViolations);
    }
}
