package eu.egm.srv.cnm.services.domain;

/**
 * Bounded Elasticsearch document containing one JSON chunk of a streamed RDF fragment.
 */
public record CnmProfileFragmentChunkDocument(
        String id,
        String importId,
        String fileId,
        Integer chunkIndex,
        String chunkJson,
        Object importedAt) {
    public CnmProfileFragmentChunkDocument {
        chunkJson = chunkJson == null ? "" : chunkJson;
    }
}
