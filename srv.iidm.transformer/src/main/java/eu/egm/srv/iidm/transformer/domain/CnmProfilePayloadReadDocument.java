package eu.egm.srv.iidm.transformer.domain;

import java.util.List;

/**
 * Read model for CNM-owned profile payload documents.
 */
public record CnmProfilePayloadReadDocument(
        String id,
        String importId,
        String fileId,
        String profileJsonType,
        String profileJson,
        List<String> profileJsonChunks,
        Object importedAt) {
    public CnmProfilePayloadReadDocument {
        profileJson = profileJson == null ? "" : profileJson;
        profileJsonChunks = profileJsonChunks == null ? List.of() : List.copyOf(profileJsonChunks);
    }
}
