package eu.egm.data.common;

/**
 * Named security-analysis parameter set persisted for repeatable runs.
 */
public record SecurityAnalysisParameterConfiguration(
        String id,
        String name,
        String source,
        String createdAt,
        String updatedAt,
        SecurityAnalysisParametersDto parameters) {
}
