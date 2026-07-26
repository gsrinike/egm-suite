package eu.egm.data.iidm.common;

import java.util.List;

/**
 * Event emitted immediately after the IIDM transformer persists a STARTED
 * transform document.
 */
public record IidmProfileTransformStarted(
        String importId,
        String fileId,
        String transformId,
        List<String> sourceFileIds,
        List<String> sourceFileNames,
        String iidmNetworkId,
        String message) {
    public IidmProfileTransformStarted {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        sourceFileNames = sourceFileNames == null ? List.of() : List.copyOf(sourceFileNames);
    }
}
