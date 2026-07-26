package eu.egm.data.common.lfsa.common;

/**
 * Application-owned wrapper for PowSyBl security-analysis parameters.
 */
public record SecurityAnalysisParametersDto(
        boolean voltageLimitsChecked,
        boolean currentLimitsChecked,
        boolean activePowerLimitsChecked,
        boolean intermediateResultsInOperatorStrategy,
        String debugDir,
        String contingencyElementType,
        int maxGeneratedContingencies) {
    public SecurityAnalysisParametersDto {
        contingencyElementType = defaultValue(contingencyElementType, "LINE");
        debugDir = debugDir == null ? "" : debugDir;
        maxGeneratedContingencies = maxGeneratedContingencies <= 0 ? 25 : maxGeneratedContingencies;
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
