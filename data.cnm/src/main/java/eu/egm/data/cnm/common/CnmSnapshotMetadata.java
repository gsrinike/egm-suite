package eu.egm.data.cnm.common;

import java.time.Instant;
import java.util.List;

/**
 * Lightweight metadata for a stitched CGM snapshot, safe for GUI list views.
 */
public record CnmSnapshotMetadata(
        String snapshotId,
        String importId,
        CnmServiceType serviceType,
        String tsoName,
        String businessDay,
        String businessTime,
        String timeFrame,
        List<String> sourceFileIds,
        Integer staticObjectCount,
        Integer relationCount,
        Integer stateValueCount,
        Integer diagnosticCount,
        Integer payloadSectionCount,
        CnmSnapshotState state,
        String message,
        Instant assembledAt) {
    public CnmSnapshotMetadata {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
    }
}
