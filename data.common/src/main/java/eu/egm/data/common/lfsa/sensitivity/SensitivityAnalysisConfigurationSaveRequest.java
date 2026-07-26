package eu.egm.data.common.lfsa.sensitivity;

/**
 * Request used by the GUI to save a sensitivity-analysis configuration.
 */
public record SensitivityAnalysisConfigurationSaveRequest(
        String name,
        SensitivityAnalysisParametersDto parameters) {
}
