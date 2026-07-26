package eu.egm.data.common.lfsa.sensitivity;

/**
 * Summary row for a sensitivity-analysis run.
 */
public record SensitivityAnalysisRunSummary(
        String runId,
        String fileImportId,
        SensitivityAnalysisRunState state,
        String runDate,
        String runTime,
        int networkCount,
        int factorCount,
        int resultCount,
        int diagnosticCount,
        String ptdfObjectId,
        String lodfObjectId,
        String glskObjectId,
        String message) {
    public SensitivityAnalysisRunSummary {
        ptdfObjectId = ptdfObjectId == null ? "" : ptdfObjectId;
        lodfObjectId = lodfObjectId == null ? "" : lodfObjectId;
        glskObjectId = glskObjectId == null ? "" : glskObjectId;
    }
}
