package eu.egm.data.iidm.common;

/**
 * Event emitted when a profile-level IIDM transformation fails.
 */
public record IidmProfileTransformFailed(
        String importId,
        String fileId,
        String message) {
}
