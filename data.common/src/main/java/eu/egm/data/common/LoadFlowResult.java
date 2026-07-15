package eu.egm.data.common;

import java.time.Instant;
import java.util.List;

public record LoadFlowResult(
        String resultId,
        WorkflowStatus status,
        List<LineFlow> lineFlows,
        Instant calculatedAt,
        String message) {
    public LoadFlowResult {
        lineFlows = lineFlows == null ? List.of() : List.copyOf(lineFlows);
    }
}
