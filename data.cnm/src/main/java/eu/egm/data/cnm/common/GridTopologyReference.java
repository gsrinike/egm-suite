package eu.egm.data.cnm.common;

/**
 * Reference between RDF subjects resolved during profile extraction.
 */
public record GridTopologyReference(
        String sourceMRID,
        String targetMRID,
        String referenceType,
        String profileType) {
}
