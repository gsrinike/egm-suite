package eu.egm.srv.cnm.services.domain;

import eu.egm.data.cnm.common.ProfileFamily;
import java.util.List;
import java.util.Map;

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
        Map<String, Long> entityCounts,
        Integer factCount,
        List<String> diagnostics,
        String fragmentJson,
        List<String> fragmentJsonChunks,
        Object importedAt) {
    public CnmProfileFragmentDocument {
        entityCounts = entityCounts == null ? Map.of() : Map.copyOf(entityCounts);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        fragmentJson = fragmentJson == null ? "" : fragmentJson;
        fragmentJsonChunks = fragmentJsonChunks == null ? List.of() : List.copyOf(fragmentJsonChunks);
    }
}
