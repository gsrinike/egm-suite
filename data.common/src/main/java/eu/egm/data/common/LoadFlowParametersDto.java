package eu.egm.data.common;

/**
 * Application-owned wrapper for PowSyBl load-flow parameters.
 */
public record LoadFlowParametersDto(
        boolean distributedSlack,
        boolean useReactiveLimits,
        boolean transformerVoltageControlOn,
        boolean phaseShifterRegulationOn,
        boolean shuntCompensatorVoltageControlOn,
        boolean readSlackBus,
        boolean writeSlackBus,
        String voltageInitMode,
        String balanceType,
        String componentMode,
        boolean hvdcAcEmulation,
        double dcPowerFactor) {
    public LoadFlowParametersDto {
        voltageInitMode = defaultValue(voltageInitMode, "PREVIOUS_VALUES");
        balanceType = defaultValue(balanceType, "PROPORTIONAL_TO_GENERATION_P");
        componentMode = defaultValue(componentMode, "MAIN_CONNECTED");
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
