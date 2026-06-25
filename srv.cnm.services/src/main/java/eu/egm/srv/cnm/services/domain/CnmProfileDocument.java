package eu.egm.srv.cnm.services.domain;

import eu.egm.data.cnm.common.ImportFileState;
import eu.egm.data.cnm.common.ProfileFamily;

/**
 * Elasticsearch document used for profile-level filtering.
 */
public record CnmProfileDocument(
        String id,
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
        Object importedAt) {
}
