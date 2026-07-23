package eu.egm.srv.cnm.services.domain;

import java.util.List;

/**
 * Searchable mRID index entry for stitched CGMES processing.
 */
public record CnmMridIndexDocument(
        String id,
        String importId,
        String mRID,
        String cimType,
        String profileType,
        String fileId,
        List<String> referenceTargets,
        Object indexedAt) {
    public CnmMridIndexDocument {
        referenceTargets = referenceTargets == null ? List.of() : List.copyOf(referenceTargets);
    }
}
