package eu.egm.srv.iidm.transformer.domain;

/**
 * Read model for CNM-owned stitched snapshot payload sections.
 */
public record CnmNetworkSnapshotPayloadReadDocument(
        String id,
        String snapshotId,
        String importId,
        String section,
        Integer sequence,
        Integer entityCount,
        String payloadJson,
        Object createdAt) {
    public CnmNetworkSnapshotPayloadReadDocument {
        section = section == null ? "" : section;
        sequence = sequence == null ? 0 : sequence;
        entityCount = entityCount == null ? 0 : entityCount;
        payloadJson = payloadJson == null ? "" : payloadJson;
    }
}
