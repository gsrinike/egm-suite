package eu.egm.srv.common.lfsa.domain;

import java.util.List;

/**
 * Read-only projection of persisted IIDM networks consumed by LFSA.
 */
public record IidmNetworkReadDocument(
        String id,
        String importId,
        List<String> sourceFileIds,
        List<String> sourceFileNames,
        String businessDay,
        String businessTime,
        String timeFrame,
        String tsoName,
        String networkFormat,
        String networkXiidm,
        List<String> networkXiidmChunks,
        String networkXiidmBucket,
        String networkXiidmObjectKey,
        Object elementCounts,
        Object createdAt,
        Object updatedAt) {
    public IidmNetworkReadDocument {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        sourceFileNames = sourceFileNames == null ? List.of() : List.copyOf(sourceFileNames);
        networkXiidm = networkXiidm == null ? "" : networkXiidm;
        networkXiidmChunks = networkXiidmChunks == null ? List.of() : List.copyOf(networkXiidmChunks);
        networkXiidmBucket = networkXiidmBucket == null ? "" : networkXiidmBucket;
        networkXiidmObjectKey = networkXiidmObjectKey == null ? "" : networkXiidmObjectKey;
    }
}
