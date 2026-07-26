package eu.egm.data.common;

import java.util.List;

/**
 * Event payload used to start asynchronous load-flow and security-analysis processing.
 */
public record SecurityAnalysisRequested(
        String runId,
        String fileImportId,
        List<String> iidmNetworkIds,
        String requestedAt) {
    public SecurityAnalysisRequested {
        iidmNetworkIds = iidmNetworkIds == null ? List.of() : List.copyOf(iidmNetworkIds);
    }
}
