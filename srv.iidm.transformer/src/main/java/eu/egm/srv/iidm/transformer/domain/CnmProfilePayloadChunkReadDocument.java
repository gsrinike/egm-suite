package eu.egm.srv.iidm.transformer.domain;

/**
 * Read model for CNM-owned profile payload chunk documents.
 */
public record CnmProfilePayloadChunkReadDocument(
        String id,
        String importId,
        String fileId,
        Integer chunkIndex,
        String profileJsonType,
        String chunkJson,
        Object importedAt) {
    public CnmProfilePayloadChunkReadDocument {
        chunkJson = chunkJson == null ? "" : chunkJson;
    }
}
