package eu.egm.data.cnm.common;

import java.time.Instant;

/**
 * Event requesting asynchronous assembly of a complete CGM snapshot for one
 * model group after all related RDF profile files have been parsed.
 */
public record CnmSnapshotAssemblyRequested(
        String importId,
        String tsoName,
        String businessDay,
        String businessTime,
        String modelTimeFrame,
        CnmServiceType serviceType,
        TimeFrame importTimeFrame,
        int retryCount,
        Instant requestedAt) {
}
