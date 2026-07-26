package eu.egm.srv.common.lfsa.config.sensitivity;

import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisParametersDto;

/**
 * Cached sensitivity-analysis defaults loaded from YAML.
 */
public record SensitivityDefaults(
        int maxSearchRuns,
        int maxDiagnostics,
        int maxResultRows,
        SensitivityAnalysisParametersDto parameters,
        String defaultPtdfObjectId,
        String defaultLodfObjectId,
        String defaultGlskObjectId) {
}
