package eu.egm.data.common;

/**
 * Request used by the GUI to persist a reusable security-analysis configuration.
 */
public record SecurityAnalysisParameterConfigurationSaveRequest(
        String name,
        SecurityAnalysisParametersDto parameters) {
}
