package eu.egm.data.common;

import java.util.List;

public record CsaStartRequest(
        String caseName,
        NetworkCaseReference networkCase,
        List<String> contingencyIds,
        boolean optimizeRemedialActions) {
    public CsaStartRequest {
        contingencyIds = contingencyIds == null ? List.of() : List.copyOf(contingencyIds);
    }
}
