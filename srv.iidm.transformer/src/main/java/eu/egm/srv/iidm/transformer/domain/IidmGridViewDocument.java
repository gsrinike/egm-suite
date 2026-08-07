package eu.egm.srv.iidm.transformer.domain;

import java.util.List;

/**
 * Metadata for a generated Grid View visualisation stored in object storage.
 */
public record IidmGridViewDocument(
        String id,
        String importId,
        String networkId,
        String bucket,
        String objectKey,
        String contentType,
        String state,
        List<String> sourceFileIds,
        int coordinateCount,
        int lineCount,
        int substationCount,
        List<String> diagnostics,
        Object generatedAt,
        Object updatedAt) {
    public IidmGridViewDocument {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
