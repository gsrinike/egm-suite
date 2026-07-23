package eu.egm.srv.cnm.services.domain;

/**
 * Bounded payload section for a stitched CGM snapshot.
 */
public record CnmNetworkSnapshotPayloadDocument(
        String id,
        String snapshotId,
        String importId,
        String section,
        Integer sequence,
        Integer entityCount,
        String payloadJson,
        Object createdAt) {
    public CnmNetworkSnapshotPayloadDocument {
        section = section == null ? "" : section;
        sequence = sequence == null ? 0 : sequence;
        entityCount = entityCount == null ? 0 : entityCount;
        payloadJson = payloadJson == null ? "" : payloadJson;
    }
}
