package eu.egm.data.common;

/**
 * Named Load Flow and Security Analysis configuration persisted for repeatable runs.
 */
public record LfSaParameterConfiguration(
        String id,
        String name,
        String source,
        String createdAt,
        String updatedAt,
        LoadFlowStrategy loadFlowStrategy,
        LoadFlowParametersDto loadFlowParameters,
        SecurityAnalysisParametersDto securityAnalysisParameters) {
}
