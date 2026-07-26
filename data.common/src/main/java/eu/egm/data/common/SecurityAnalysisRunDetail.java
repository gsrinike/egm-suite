package eu.egm.data.common;

import java.util.List;
import java.util.Map;

/**
 * Full view of a security-analysis run, including table-ready result sections.
 */
public record SecurityAnalysisRunDetail(
        SecurityAnalysisRunSummary summary,
        List<LineFlow> lineFlows,
        List<ContingencyViolation> violations,
        Map<String, Long> networkElementCounts,
        List<String> diagnostics) {
    public SecurityAnalysisRunDetail {
        lineFlows = lineFlows == null ? List.of() : List.copyOf(lineFlows);
        violations = violations == null ? List.of() : List.copyOf(violations);
        networkElementCounts = networkElementCounts == null ? Map.of() : Map.copyOf(networkElementCounts);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
