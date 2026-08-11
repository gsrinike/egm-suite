package eu.egm.srv.cnm.services.domain;

/**
 * Bounded Elasticsearch document containing one JSON chunk of a parsed profile payload.
 *
 * <p>The parent payload document intentionally stays small. Large EQ/GL profiles can
 * exceed Elasticsearch request limits if all chunks are embedded in one document.</p>
 */
public record CnmProfilePayloadChunkDocument(
        String id,
        String importId,
        String fileId,
        Integer chunkIndex,
        String profileJsonType,
        String chunkJson,
        Object importedAt) {
    public CnmProfilePayloadChunkDocument {
        chunkJson = chunkJson == null ? "" : chunkJson;
    }
}
