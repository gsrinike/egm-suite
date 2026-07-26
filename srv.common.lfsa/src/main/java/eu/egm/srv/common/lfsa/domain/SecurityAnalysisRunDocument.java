package eu.egm.srv.common.lfsa.domain;

import eu.egm.data.common.lfsa.common.ContingencyViolation;
import eu.egm.data.common.lfsa.common.AnalysisStepState;
import eu.egm.data.common.lfsa.common.LfSaParameterConfiguration;
import eu.egm.data.common.lfsa.common.LineFlow;
import eu.egm.data.common.lfsa.common.LoadFlowComputationResult;
import eu.egm.data.common.lfsa.common.LoadFlowParametersDto;
import eu.egm.data.common.lfsa.common.LoadFlowStrategy;
import eu.egm.data.common.lfsa.common.SecurityAnalysisComputationResult;
import eu.egm.data.common.lfsa.common.SecurityAnalysisParametersDto;
import eu.egm.data.common.lfsa.common.SecurityAnalysisRunState;
import java.util.List;
import java.util.Map;

/**
 * LFSA-owned persistence document for a security-analysis execution.
 */
public record SecurityAnalysisRunDocument(
        String id,
        String fileImportId,
        SecurityAnalysisRunState state,
        Object startedAt,
        Object completedAt,
        Object failedAt,
        AnalysisStepState loadFlowState,
        AnalysisStepState securityAnalysisState,
        String parameterConfigurationId,
        String parameterConfigurationName,
        LoadFlowStrategy loadFlowStrategy,
        LoadFlowParametersDto loadFlowParameters,
        SecurityAnalysisParametersDto securityAnalysisParameters,
        LoadFlowComputationResult loadFlowResult,
        SecurityAnalysisComputationResult computationResult,
        List<String> iidmNetworkIds,
        Map<String, Long> networkElementCounts,
        List<LineFlow> lineFlows,
        List<ContingencyViolation> violations,
        List<String> diagnostics,
        String message) {
    public SecurityAnalysisRunDocument {
        loadFlowState = loadFlowState == null ? AnalysisStepState.NOT_STARTED : loadFlowState;
        securityAnalysisState = securityAnalysisState == null ? AnalysisStepState.NOT_STARTED : securityAnalysisState;
        parameterConfigurationId = parameterConfigurationId == null ? "" : parameterConfigurationId;
        parameterConfigurationName = parameterConfigurationName == null ? "" : parameterConfigurationName;
        loadFlowStrategy = loadFlowStrategy == null ? LoadFlowStrategy.DC_ONLY : loadFlowStrategy;
        loadFlowParameters = loadFlowParameters == null ? defaultLoadFlowParameters() : loadFlowParameters;
        securityAnalysisParameters = securityAnalysisParameters == null
                ? defaultSecurityAnalysisParameters()
                : securityAnalysisParameters;
        iidmNetworkIds = iidmNetworkIds == null ? List.of() : List.copyOf(iidmNetworkIds);
        networkElementCounts = networkElementCounts == null ? Map.of() : Map.copyOf(networkElementCounts);
        lineFlows = lineFlows == null ? List.of() : List.copyOf(lineFlows);
        violations = violations == null ? List.of() : List.copyOf(violations);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public LfSaParameterConfiguration parameterConfiguration() {
        return new LfSaParameterConfiguration(
                parameterConfigurationId,
                parameterConfigurationName,
                parameterConfigurationId.isBlank() ? "DEFAULT" : "USER",
                "",
                "",
                loadFlowStrategy,
                loadFlowParameters,
                securityAnalysisParameters);
    }

    private static LoadFlowParametersDto defaultLoadFlowParameters() {
        return new LoadFlowParametersDto(
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                "PREVIOUS_VALUES",
                "PROPORTIONAL_TO_GENERATION_P",
                "MAIN_CONNECTED",
                true,
                1.0);
    }

    private static SecurityAnalysisParametersDto defaultSecurityAnalysisParameters() {
        return new SecurityAnalysisParametersDto(true, true, true, false, "", "LINE", 25);
    }
}
