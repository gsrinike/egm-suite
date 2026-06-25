package eu.egm.data.cnm.common;

/**
 * Status update reported by downstream file processing.
 */
public record ImportFileStatusUpdateRequest(
        ImportFileState state,
        String message) {
}
