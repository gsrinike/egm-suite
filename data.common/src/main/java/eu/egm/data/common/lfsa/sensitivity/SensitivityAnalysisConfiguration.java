package eu.egm.data.common.lfsa.sensitivity;

/**
 * Named sensitivity-analysis configuration persisted for repeatable runs.
 */
public record SensitivityAnalysisConfiguration(
        String id,
        String name,
        String source,
        String createdAt,
        String updatedAt,
        SensitivityAnalysisParametersDto parameters) {
}
