package eu.egm.data.iidm.common;

/**
 * Event emitted when a profile-level IIDM projection has been persisted.
 */
public record IidmProfileTransformCompleted(
        String importId,
        String fileId,
        String iidmNetworkId,
        String message) {
}
