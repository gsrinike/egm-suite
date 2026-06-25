package eu.egm.data.cnm.common;

import java.time.Instant;
import java.util.List;

/**
 * Status and metadata for one uploaded RDF payload.
 */
public record ImportFileStatus(
        String fileId,
        String fileName,
        String objectId,
        ImportFileState state,
        ProfileFamily profileFamily,
        String businessDay,
        String businessTime,
        String modelTimeFrame,
        String tsoName,
        String profileType,
        String modelVersion,
        List<RdfProfileReference> profiles,
        String message,
        Instant uploadedAt) {
    public ImportFileStatus {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }
}
