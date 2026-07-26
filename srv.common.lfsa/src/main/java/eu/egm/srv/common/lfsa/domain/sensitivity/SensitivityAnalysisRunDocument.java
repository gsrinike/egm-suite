package eu.egm.srv.common.lfsa.domain.sensitivity;

import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisConfiguration;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisParametersDto;
import eu.egm.data.common.lfsa.sensitivity.SensitivityAnalysisRunState;
import eu.egm.data.common.lfsa.sensitivity.SensitivityFactorDto;
import eu.egm.data.common.lfsa.sensitivity.SensitivityMatrixRow;
import java.util.List;
import java.util.Map;

/**
 * Document-store representation of an asynchronous sensitivity-analysis run.
 */
public record SensitivityAnalysisRunDocument(
        String id,
        String fileImportId,
        SensitivityAnalysisRunState state,
        Object startedAt,
        Object completedAt,
        Object failedAt,
        String configurationId,
        String configurationName,
        SensitivityAnalysisParametersDto parameters,
        List<String> iidmNetworkIds,
        Map<String, String> inputReferences,
        List<SensitivityFactorDto> factors,
        List<SensitivityMatrixRow> matrixRows,
        Map<String, Long> networkElementCounts,
        List<String> diagnostics,
        String message) {
    public SensitivityAnalysisRunDocument {
        state = state == null ? SensitivityAnalysisRunState.STARTED : state;
        configurationId = configurationId == null ? "" : configurationId;
        configurationName = configurationName == null ? "" : configurationName;
        iidmNetworkIds = iidmNetworkIds == null ? List.of() : List.copyOf(iidmNetworkIds);
        inputReferences = inputReferences == null ? Map.of() : Map.copyOf(inputReferences);
        factors = factors == null ? List.of() : List.copyOf(factors);
        matrixRows = matrixRows == null ? List.of() : List.copyOf(matrixRows);
        networkElementCounts = networkElementCounts == null ? Map.of() : Map.copyOf(networkElementCounts);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public SensitivityAnalysisConfiguration configuration() {
        return new SensitivityAnalysisConfiguration(
                configurationId,
                configurationName,
                configurationId.isBlank() ? "DEFAULT" : "USER",
                "",
                "",
                parameters);
    }
}
