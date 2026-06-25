package eu.egm.data.cnm.common;

/**
 * Profile URI discovered from RDF metadata such as dcterms:conformsTo.
 */
public record RdfProfileReference(
        ProfileFamily family,
        String uri,
        String version) {
}
