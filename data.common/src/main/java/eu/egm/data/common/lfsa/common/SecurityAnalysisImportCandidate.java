package eu.egm.data.common.lfsa.common;

/**
 * Import that can be used as input for a security-analysis run.
 */
public record SecurityAnalysisImportCandidate(
        String importId,
        String service,
        String timeFrame,
        String state,
        String createdAt,
        String businessDay,
        String message) {
}
