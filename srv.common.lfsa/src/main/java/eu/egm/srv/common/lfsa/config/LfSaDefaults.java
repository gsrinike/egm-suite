package eu.egm.srv.common.lfsa.config;

import eu.egm.data.common.LoadFlowParametersDto;
import eu.egm.data.common.LoadFlowStrategy;
import eu.egm.data.common.SecurityAnalysisParametersDto;

/**
 * Cached LFSA defaults loaded from the module default YAML.
 */
public record LfSaDefaults(
        int maxSearchImports,
        int maxSearchRuns,
        int maxDiagnostics,
        int maxLineFlows,
        int maxIidmNetworks,
        LoadFlowStrategy loadFlowStrategy,
        LoadFlowParametersDto loadFlowParameters,
        SecurityAnalysisParametersDto securityAnalysisParameters) {
}
