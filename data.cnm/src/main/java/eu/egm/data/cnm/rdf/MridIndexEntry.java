package eu.egm.data.cnm.rdf;

import java.util.List;

/**
 * Searchable entry in the central mRID index for a parsed import.
 */
public record MridIndexEntry(
        String importId,
        String mRID,
        String cimType,
        String profileType,
        String fileId,
        List<String> referenceTargets) {
    public MridIndexEntry {
        referenceTargets = referenceTargets == null ? List.of() : List.copyOf(referenceTargets);
    }
}
