package eu.egm.data.common.lfsa.sensitivity;

/**
 * Table-ready description of one sensitivity factor.
 */
public record SensitivityFactorDto(
        String functionType,
        String functionId,
        String variableType,
        String variableId,
        String contingencyContext) {
}
