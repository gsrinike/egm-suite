package eu.egm.data.cnm.iidm;

/**
 * Lightweight IIDM element projection for API and UI use.
 */
public record IidmElementSummary(
        String id,
        String name,
        IidmElementType type,
        String containerId,
        double nominalVoltage) {
}
