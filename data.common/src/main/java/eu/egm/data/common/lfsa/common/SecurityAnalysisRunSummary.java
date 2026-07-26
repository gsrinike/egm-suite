package eu.egm.data.common.lfsa.common;

/**
 * Searchable summary of a security-analysis run.
 */
public record SecurityAnalysisRunSummary(
        String runId,
        String fileImportId,
        SecurityAnalysisRunState state,
        AnalysisStepState loadFlowState,
        AnalysisStepState securityAnalysisState,
        String runDate,
        String runTime,
        int networkCount,
        int lineFlowCount,
        int violationCount,
        int diagnosticCount,
        String message) {
}
