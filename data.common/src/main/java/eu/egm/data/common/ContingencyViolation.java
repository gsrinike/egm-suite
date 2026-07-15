package eu.egm.data.common;

public record ContingencyViolation(
        String contingencyId,
        String elementId,
        ViolationType violationType,
        double observedValue,
        double limitValue,
        String unit,
        String severity) {
}
