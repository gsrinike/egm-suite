package eu.egm.srv.common.lfsa.domain;

import eu.egm.data.common.LoadFlowParametersDto;
import eu.egm.data.common.LoadFlowStrategy;
import eu.egm.data.common.SecurityAnalysisParametersDto;

/**
 * LFSA-owned document for named PowSyBl security-analysis parameter sets.
 */
public record SecurityAnalysisParameterConfigurationDocument(
        String id,
        String name,
        String source,
        Object createdAt,
        Object updatedAt,
        LoadFlowStrategy loadFlowStrategy,
        LoadFlowParametersDto loadFlowParameters,
        SecurityAnalysisParametersDto securityAnalysisParameters) {
}
