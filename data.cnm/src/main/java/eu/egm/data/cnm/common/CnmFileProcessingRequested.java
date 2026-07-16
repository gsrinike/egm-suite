package eu.egm.data.cnm.common;

import java.time.Instant;

/**
 * Event requesting asynchronous metadata extraction for one stored CNM file.
 */
public record CnmFileProcessingRequested(
        String importId,
        String fileId,
        String objectId,
        String fileName,
        CnmServiceType serviceType,
        TimeFrame timeFrame,
        int retryCount,
        Instant requestedAt) {
}
