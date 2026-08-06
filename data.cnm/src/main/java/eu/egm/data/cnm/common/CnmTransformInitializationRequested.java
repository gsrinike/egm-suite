package eu.egm.data.cnm.common;

import java.time.Instant;

/**
 * Event emitted after raw import files have been persisted in object storage.
 * Downstream CNM processing uses it to classify model groups and enqueue RDF
 * metadata extraction work.
 */
public record CnmTransformInitializationRequested(
        String importId,
        CnmServiceType serviceType,
        TimeFrame timeFrame,
        int retryCount,
        Instant requestedAt) {
}
