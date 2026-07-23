package eu.egm.srv.iidm.transformer.domain;

import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.CnmSnapshotState;
import java.util.List;

/**
 * Metadata-only read model for CNM-owned stitched network snapshot documents.
 */
public record CnmNetworkSnapshotReadDocument(
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
    public CnmNetworkSnapshotReadDocument {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        state = state == null ? CnmSnapshotState.STARTED : state;
        message = message == null ? "" : message;
    }
}
