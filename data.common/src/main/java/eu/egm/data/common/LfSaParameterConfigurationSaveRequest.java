package eu.egm.data.common;

/**
 * Request used by the GUI to persist a reusable LFnSA configuration.
 */
public record LfSaParameterConfigurationSaveRequest(
        String name,
        LoadFlowStrategy loadFlowStrategy,
        LoadFlowParametersDto loadFlowParameters,
        SecurityAnalysisParametersDto securityAnalysisParameters) {
}
