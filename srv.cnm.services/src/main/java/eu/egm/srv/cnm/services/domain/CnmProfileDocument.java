package eu.egm.srv.cnm.services.domain;

import eu.egm.data.cnm.common.ImportFileState;
import eu.egm.data.cnm.common.ProfileFamily;
import java.util.List;

/**
 * Elasticsearch document used for profile-level filtering.
 */
public record CnmProfileDocument(
        String id,
        String importId,
        String fileId,
        String fileName,
        String objectId,
        ImportFileState state,
        ProfileFamily profileFamily,
        String profileType,
        String detectedProfileKind,
        String modelId,
        String tsoName,
        String businessDay,
        String businessTime,
        String timeFrame,
        String version,
        List<CnmEntityCountDocument> entityCounts,
        Integer warningCount,
        Integer errorCount,
        String profileJsonType,
        String profileJson,
        List<String> profileJsonChunks,
        Object importedAt) {
    public CnmProfileDocument {
        entityCounts = entityCounts == null ? List.of() : List.copyOf(entityCounts);
        profileJsonChunks = profileJsonChunks == null ? List.of() : List.copyOf(profileJsonChunks);
    }

    /**
     * Stable key/value count shape avoids Elasticsearch dynamic mapping conflicts.
     */
    public record CnmEntityCountDocument(String entityType, long count) {
    }
}
