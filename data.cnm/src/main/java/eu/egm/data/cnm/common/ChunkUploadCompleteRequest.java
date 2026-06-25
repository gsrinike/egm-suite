package eu.egm.data.cnm.common;

/**
 * Completes a previously chunked import and starts model processing.
 */
public record ChunkUploadCompleteRequest(
        String importId,
        CnmServiceType serviceType,
        TimeFrame timeFrame,
        String message) {
}
