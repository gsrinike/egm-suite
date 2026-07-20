package eu.egm.srv.iidm.transformer.api;

import java.util.List;

/**
 * Lightweight network response used by list screens. It intentionally excludes
 * the XIIDM payload and chunks.
 */
public record IidmNetworkSummaryResponse(
        String networkId,
        String importId,
        List<String> sourceFileIds,
        String businessDay,
        String businessTime,
        String timeFrame,
        String tsoName,
        String networkFormat,
        List<IidmElementCountResponse> elementCounts,
        Object createdAt,
        Object updatedAt) {
    public IidmNetworkSummaryResponse {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        elementCounts = elementCounts == null ? List.of() : List.copyOf(elementCounts);
    }
}
