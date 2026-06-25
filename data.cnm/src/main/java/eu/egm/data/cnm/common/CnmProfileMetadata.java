package eu.egm.data.cnm.common;

import java.time.Instant;

/**
 * Searchable metadata for one imported CGMES or NCP profile.
 */
public record CnmProfileMetadata(
        String profileId,
        String importId,
        String fileName,
        String objectId,
        ImportFileState state,
        ProfileFamily profileFamily,
        String profileType,
        String tsoName,
        String businessDay,
        String businessTime,
        String timeFrame,
        String version,
        Instant importedAt) {
}
