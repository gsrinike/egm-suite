package eu.egm.srv.iidm.transformer.domain;

import java.util.List;

/**
 * Read model for CNM-owned profile payload documents.
 */
public record CnmProfilePayloadReadDocument(
        String id,
        String importId,
        String fileId,
        String profileJsonType,
        String profileJson,
        List<String> profileJsonChunks,
        Integer profileJsonChunkCount,
        String payloadBucket,
        String payloadObjectKey,
        String payloadContentType,
        String payloadChecksum,
        Long payloadSizeBytes,
        Object importedAt) {
    public CnmProfilePayloadReadDocument {
        profileJson = profileJson == null ? "" : profileJson;
        profileJsonChunks = profileJsonChunks == null ? List.of() : List.copyOf(profileJsonChunks);
        profileJsonChunkCount = profileJsonChunkCount == null ? profileJsonChunks.size() : profileJsonChunkCount;
        payloadBucket = payloadBucket == null ? "" : payloadBucket;
        payloadObjectKey = payloadObjectKey == null ? "" : payloadObjectKey;
        payloadContentType = payloadContentType == null ? "" : payloadContentType;
        payloadChecksum = payloadChecksum == null ? "" : payloadChecksum;
        payloadSizeBytes = payloadSizeBytes == null ? 0L : payloadSizeBytes;
    }
}
