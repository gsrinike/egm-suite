package eu.egm.srv.iidm.transformer.api;

import java.util.List;

/**
 * Generated Grid View map artifact returned to the GUI for inline rendering.
 */
public record IidmGridViewMapResponse(
        String id,
        String importId,
        String networkId,
        String bucket,
        String objectKey,
        String contentType,
        String state,
        String svg,
        int coordinateCount,
        int lineCount,
        int substationCount,
        List<String> diagnostics,
        Object generatedAt) {
    public IidmGridViewMapResponse {
        svg = svg == null ? "" : svg;
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
