package eu.egm.data.common.lfsa.sensitivity;

import java.util.List;

/**
 * Event emitted when a sensitivity-analysis run is ready for asynchronous processing.
 */
public record SensitivityAnalysisRequested(
        String runId,
        String fileImportId,
        List<String> iidmNetworkIds,
        String requestedAt) {
    public SensitivityAnalysisRequested {
        iidmNetworkIds = iidmNetworkIds == null ? List.of() : List.copyOf(iidmNetworkIds);
    }
}
