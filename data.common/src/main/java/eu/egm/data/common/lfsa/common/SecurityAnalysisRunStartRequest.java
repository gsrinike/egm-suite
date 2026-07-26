package eu.egm.data.common.lfsa.common;

/**
 * Request to start a security-analysis run for one completed CNM import.
 */
public record SecurityAnalysisRunStartRequest(
        String fileImportId,
        String parameterConfigurationId) {
}
