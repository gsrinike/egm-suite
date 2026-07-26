package eu.egm.data.common.lfsa.sensitivity;

import java.util.List;
import java.util.Map;

/**
 * Full sensitivity-analysis run detail with table-ready result sections.
 */
public record SensitivityAnalysisRunDetail(
        SensitivityAnalysisRunSummary summary,
        SensitivityAnalysisConfiguration configuration,
        List<String> iidmNetworkIds,
        Map<String, String> inputReferences,
        List<SensitivityFactorDto> factors,
        List<SensitivityMatrixRow> matrixRows,
        Map<String, Long> networkElementCounts,
        List<String> diagnostics) {
    public SensitivityAnalysisRunDetail {
        iidmNetworkIds = iidmNetworkIds == null ? List.of() : List.copyOf(iidmNetworkIds);
        inputReferences = inputReferences == null ? Map.of() : Map.copyOf(inputReferences);
        factors = factors == null ? List.of() : List.copyOf(factors);
        matrixRows = matrixRows == null ? List.of() : List.copyOf(matrixRows);
        networkElementCounts = networkElementCounts == null ? Map.of() : Map.copyOf(networkElementCounts);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
