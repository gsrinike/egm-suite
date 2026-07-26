package eu.egm.data.common;

import java.util.List;
import java.util.Map;

/**
 * Bounded DTO projection of PowSyBl load-flow execution.
 */
public record LoadFlowComputationResult(
        boolean succeeded,
        String status,
        int componentCount,
        List<String> componentStatuses,
        Map<String, String> metrics,
        String logs) {
    public LoadFlowComputationResult {
        componentStatuses = componentStatuses == null ? List.of() : List.copyOf(componentStatuses);
        metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
        logs = logs == null ? "" : logs;
    }
}
