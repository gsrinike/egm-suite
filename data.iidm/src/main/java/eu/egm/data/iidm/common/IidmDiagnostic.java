package eu.egm.data.iidm.common;

/**
 * Diagnostic message produced during IIDM transformation.
 */
public record IidmDiagnostic(String severity, String code, String message, String sourceId) {
}
