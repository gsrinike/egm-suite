package eu.egm.data.common;

import java.util.List;

public record SecurityAnalysisRequest(
        String requestId,
        NetworkCaseReference networkCase,
        List<String> contingencyIds,
        LoadFlowResult baseLoadFlow) {
    public SecurityAnalysisRequest {
        contingencyIds = contingencyIds == null ? List.of() : List.copyOf(contingencyIds);
    }
}
