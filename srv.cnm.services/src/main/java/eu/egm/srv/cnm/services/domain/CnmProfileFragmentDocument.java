package eu.egm.srv.cnm.services.domain;

import eu.egm.data.cnm.common.ProfileFamily;
import java.util.List;

/**
 * Elasticsearch document for one streamed RDF profile fragment.
 */
public record CnmProfileFragmentDocument(
        String id,
        String importId,
        String fileId,
        String objectId,
        String modelId,
        ProfileFamily profileFamily,
        String profileType,
        String tsoName,
        String businessDay,
        String businessTime,
        String timeFrame,
        String version,
        List<CnmFragmentEntityCountDocument> entityCounts,
        Integer factCount,
        List<String> diagnostics,
        String fragmentJson,
        List<String> fragmentJsonChunks,
        Integer fragmentJsonChunkCount,
        String payloadBucket,
        String payloadObjectKey,
        String payloadContentType,
        String payloadChecksum,
        Long payloadSizeBytes,
        Object importedAt) {
    public CnmProfileFragmentDocument {
        entityCounts = entityCounts == null ? List.of() : List.copyOf(entityCounts);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        fragmentJson = fragmentJson == null ? "" : fragmentJson;
        fragmentJsonChunks = fragmentJsonChunks == null ? List.of() : List.copyOf(fragmentJsonChunks);
        fragmentJsonChunkCount = fragmentJsonChunkCount == null ? fragmentJsonChunks.size() : fragmentJsonChunkCount;
        payloadBucket = payloadBucket == null ? "" : payloadBucket;
        payloadObjectKey = payloadObjectKey == null ? "" : payloadObjectKey;
        payloadContentType = payloadContentType == null ? "" : payloadContentType;
        payloadChecksum = payloadChecksum == null ? "" : payloadChecksum;
        payloadSizeBytes = payloadSizeBytes == null ? 0L : payloadSizeBytes;
    }

    /**
     * Stable key/value shape avoids dynamic Elasticsearch fields for every CIM
     * entity name observed while parsing a profile.
     */
    public record CnmFragmentEntityCountDocument(String entityType, long count) {
    }
}
