package eu.egm.data.common;

import java.time.Instant;
import java.util.List;

public record RaoResult(
        String resultId,
        WorkflowStatus status,
        List<RemedialAction> actions,
        double beforeMaxLoadingPercent,
        double afterOptimizationMaxLoadingPercent,
        double afterAcValidationMaxLoadingPercent,
        Instant calculatedAt,
        String message) {
    public RaoResult {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
