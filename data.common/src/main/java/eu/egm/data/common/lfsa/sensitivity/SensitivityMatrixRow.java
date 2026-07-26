package eu.egm.data.common.lfsa.sensitivity;

/**
 * Table-ready sensitivity matrix coefficient.
 */
public record SensitivityMatrixRow(
        String functionType,
        String functionId,
        String variableType,
        String variableId,
        String contingencyId,
        double value,
        double referenceValue) {
}
