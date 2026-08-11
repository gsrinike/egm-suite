package eu.egm.srv.iidm.transformer.domain;

import java.util.List;

/**
 * PowSyBl IIDM network document owned by the IIDM transformer service.
 */
public record IidmNetworkDocument(
        String id,
        String importId,
        List<String> sourceFileIds,
        String businessDay,
        String businessTime,
        String timeFrame,
        String tsoName,
        String networkFormat,
        String networkXiidm,
        List<String> networkXiidmChunks,
        String networkXiidmBucket,
        String networkXiidmObjectKey,
        String networkXiidmChecksum,
        Long networkXiidmSizeBytes,
        String networkJsonType,
        String networkJson,
        List<String> networkJsonChunks,
        String networkJsonBucket,
        String networkJsonObjectKey,
        String networkJsonChecksum,
        Long networkJsonSizeBytes,
        List<IidmElementCountDocument> elementCounts,
        Object createdAt,
        Object updatedAt) {
    public IidmNetworkDocument {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        networkXiidm = networkXiidm == null ? "" : networkXiidm;
        networkXiidmChunks = networkXiidmChunks == null ? List.of() : List.copyOf(networkXiidmChunks);
        networkXiidmBucket = networkXiidmBucket == null ? "" : networkXiidmBucket;
        networkXiidmObjectKey = networkXiidmObjectKey == null ? "" : networkXiidmObjectKey;
        networkXiidmChecksum = networkXiidmChecksum == null ? "" : networkXiidmChecksum;
        networkXiidmSizeBytes = networkXiidmSizeBytes == null ? 0L : networkXiidmSizeBytes;
        networkJsonType = networkJsonType == null ? "" : networkJsonType;
        networkJson = networkJson == null ? "" : networkJson;
        networkJsonChunks = networkJsonChunks == null ? List.of() : List.copyOf(networkJsonChunks);
        networkJsonBucket = networkJsonBucket == null ? "" : networkJsonBucket;
        networkJsonObjectKey = networkJsonObjectKey == null ? "" : networkJsonObjectKey;
        networkJsonChecksum = networkJsonChecksum == null ? "" : networkJsonChecksum;
        networkJsonSizeBytes = networkJsonSizeBytes == null ? 0L : networkJsonSizeBytes;
        elementCounts = elementCounts == null ? List.of() : List.copyOf(elementCounts);
    }

    public record IidmElementCountDocument(String elementType, long count) {
    }
}
