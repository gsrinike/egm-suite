package eu.egm.srv.common.lfsa.domain.sensitivity;

import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisParametersDto;

/**
 * Document-store representation of a named sensitivity-analysis configuration.
 */
public record SensitivityAnalysisConfigurationDocument(
        String id,
        String name,
        String source,
        Object createdAt,
        Object updatedAt,
        SensitivityAnalysisParametersDto parameters) {
}
