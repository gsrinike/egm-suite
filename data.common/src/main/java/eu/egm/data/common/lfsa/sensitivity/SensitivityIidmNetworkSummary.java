package eu.egm.data.common.lfsa.sensitivity;

import java.util.List;

/**
 * Lightweight IIDM network option used when selecting networks for a
 * sensitivity-analysis run.
 */
public record SensitivityIidmNetworkSummary(
        String id,
        String importId,
        List<String> sourceFileIds,
        List<String> sourceFileNames,
        String businessDay,
        String businessTime,
        String timeFrame,
        String tsoName,
        String networkFormat) {
    public SensitivityIidmNetworkSummary {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        sourceFileNames = sourceFileNames == null ? List.of() : List.copyOf(sourceFileNames);
    }
}
