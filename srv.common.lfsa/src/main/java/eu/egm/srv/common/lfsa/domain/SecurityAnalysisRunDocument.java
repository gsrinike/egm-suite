package eu.egm.srv.common.lfsa.domain;

import eu.egm.data.common.ContingencyViolation;
import eu.egm.data.common.LineFlow;
import eu.egm.data.common.SecurityAnalysisRunState;
import java.util.List;
import java.util.Map;

/**
 * LFSA-owned persistence document for a security-analysis execution.
 */
public record SecurityAnalysisRunDocument(
        String id,
        String fileImportId,
        SecurityAnalysisRunState state,
        Object startedAt,
        Object completedAt,
        Object failedAt,
        List<String> iidmNetworkIds,
        Map<String, Long> networkElementCounts,
        List<LineFlow> lineFlows,
        List<ContingencyViolation> violations,
        List<String> diagnostics,
        String message) {
    public SecurityAnalysisRunDocument {
        iidmNetworkIds = iidmNetworkIds == null ? List.of() : List.copyOf(iidmNetworkIds);
        networkElementCounts = networkElementCounts == null ? Map.of() : Map.copyOf(networkElementCounts);
        lineFlows = lineFlows == null ? List.of() : List.copyOf(lineFlows);
        violations = violations == null ? List.of() : List.copyOf(violations);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
