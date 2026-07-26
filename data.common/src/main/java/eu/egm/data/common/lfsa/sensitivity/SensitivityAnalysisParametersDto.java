package eu.egm.data.common.lfsa.sensitivity;

/**
 * Application-owned wrapper for PowSyBl sensitivity-analysis parameters and factor generation rules.
 */
public record SensitivityAnalysisParametersDto(
        boolean dc,
        String functionType,
        String variableType,
        String contingencyContext,
        int maxMonitoredBranches,
        int maxVariables,
        int maxGeneratedContingencies,
        double flowFlowSensitivityValueThreshold,
        double voltageVoltageSensitivityValueThreshold,
        double flowVoltageSensitivityValueThreshold,
        double angleFlowSensitivityValueThreshold,
        String operatorStrategiesCalculationMode,
        String debugDir) {
    public SensitivityAnalysisParametersDto {
        functionType = defaultValue(functionType, "BRANCH_ACTIVE_POWER_1");
        variableType = defaultValue(variableType, "INJECTION_ACTIVE_POWER");
        contingencyContext = defaultValue(contingencyContext, "ALL");
        operatorStrategiesCalculationMode = defaultValue(operatorStrategiesCalculationMode, "ALL");
        debugDir = debugDir == null ? "" : debugDir;
        maxMonitoredBranches = maxMonitoredBranches <= 0 ? 25 : maxMonitoredBranches;
        maxVariables = maxVariables <= 0 ? 25 : maxVariables;
        maxGeneratedContingencies = maxGeneratedContingencies <= 0 ? 25 : maxGeneratedContingencies;
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
