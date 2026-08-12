package eu.egm.data.iidm.common;

/**
 * Signals that merged IIDM network creation failed.
 */
public record IidmNetworkMergeFailed(
        String importId,
        String mergedNetworkId,
        String failedAt,
        String message) {
}
