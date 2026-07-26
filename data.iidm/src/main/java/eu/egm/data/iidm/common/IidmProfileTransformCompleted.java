package eu.egm.data.iidm.common;

import java.util.List;

/**
 * Event emitted when a profile-level IIDM projection has been persisted.
 */
public record IidmProfileTransformCompleted(
        String importId,
        String fileId,
        String transformId,
        List<String> sourceFileIds,
        List<String> sourceFileNames,
        String iidmNetworkId,
        String message) {
    public IidmProfileTransformCompleted(String importId, String fileId, String iidmNetworkId, String message) {
        this(importId, fileId, "", List.of(), List.of(), iidmNetworkId, message);
    }

    public IidmProfileTransformCompleted {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        sourceFileNames = sourceFileNames == null ? List.of() : List.copyOf(sourceFileNames);
    }
}
