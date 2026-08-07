package eu.egm.srv.iidm.transformer.domain;

import eu.egm.data.cnm.common.ImportFileState;
import eu.egm.data.cnm.common.ProfileFamily;
import java.util.List;

/**
 * Lightweight read model for CNM-owned profile metadata.
 *
 * <p>The IIDM transformer uses this index to discover GL profile files without
 * loading the large JSON payload index for every file in an import.</p>
 */
public record CnmProfileReadDocument(
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
        List<CnmEntityCountReadDocument> entityCounts,
        Integer warningCount,
        Integer errorCount,
        String profileJsonType,
        Object importedAt) {
    public CnmProfileReadDocument {
        entityCounts = entityCounts == null ? List.of() : List.copyOf(entityCounts);
    }

    public record CnmEntityCountReadDocument(String entityType, long count) {
    }
}
