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
        List<IidmElementCountDocument> elementCounts,
        Object createdAt,
        Object updatedAt) {
    public IidmNetworkDocument {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        networkXiidm = networkXiidm == null ? "" : networkXiidm;
        networkXiidmChunks = networkXiidmChunks == null ? List.of() : List.copyOf(networkXiidmChunks);
        elementCounts = elementCounts == null ? List.of() : List.copyOf(elementCounts);
    }

    public record IidmElementCountDocument(String elementType, long count) {
    }
}
