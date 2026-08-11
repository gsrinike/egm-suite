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
        String payloadBucket,
        String payloadObjectKey,
        String payloadContentType,
        String payloadChecksum,
        Long payloadSizeBytes,
        Object createdAt) {
    public CnmNetworkSnapshotPayloadDocument {
        section = section == null ? "" : section;
        sequence = sequence == null ? 0 : sequence;
        entityCount = entityCount == null ? 0 : entityCount;
        payloadJson = payloadJson == null ? "" : payloadJson;
        payloadBucket = payloadBucket == null ? "" : payloadBucket;
        payloadObjectKey = payloadObjectKey == null ? "" : payloadObjectKey;
        payloadContentType = payloadContentType == null ? "" : payloadContentType;
        payloadChecksum = payloadChecksum == null ? "" : payloadChecksum;
        payloadSizeBytes = payloadSizeBytes == null ? 0L : payloadSizeBytes;
    }
}
