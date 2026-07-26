package eu.egm.data.common.lfsa.common;

import java.util.List;

/**
 * Bounded DTO projection of PowSyBl's security-analysis result.
 */
public record SecurityAnalysisComputationResult(
        boolean succeeded,
        String preContingencyStatus,
        int contingencyCount,
        List<String> postContingencyStatuses,
        List<ContingencyViolation> preContingencyViolations,
        List<ContingencyViolation> postContingencyViolations) {
    public SecurityAnalysisComputationResult {
        postContingencyStatuses = postContingencyStatuses == null ? List.of() : List.copyOf(postContingencyStatuses);
        preContingencyViolations = preContingencyViolations == null ? List.of() : List.copyOf(preContingencyViolations);
        postContingencyViolations = postContingencyViolations == null ? List.of() : List.copyOf(postContingencyViolations);
    }
}
