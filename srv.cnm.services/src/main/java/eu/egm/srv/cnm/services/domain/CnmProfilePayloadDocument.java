package eu.egm.srv.cnm.services.domain;

import java.util.List;

/**
 * Elasticsearch document that stores the large JSON payload for one parsed profile file.
 *
 * <p>Profile payloads are intentionally separated from {@link CnmProfileDocument}
 * so metadata searches never load large JSON fields into the service heap.</p>
 */
public record CnmProfilePayloadDocument(
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
    public CnmProfilePayloadDocument {
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
