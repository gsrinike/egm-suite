package eu.egm.srv.cnm.services.domain;

import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.CnmSnapshotState;
import java.util.List;

/**
 * Lightweight stitched CGM snapshot metadata used by GUI and downstream workers.
 */
public record CnmNetworkSnapshotDocument(
        String id,
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
        Object assembledAt) {
    public CnmNetworkSnapshotDocument {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        state = state == null ? CnmSnapshotState.STARTED : state;
        message = message == null ? "" : message;
    }
}
