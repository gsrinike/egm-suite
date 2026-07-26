package eu.egm.data.iidm.common;

import java.util.List;

/**
 * Event emitted when a profile-level IIDM transformation fails.
 */
public record IidmProfileTransformFailed(
        String importId,
        String fileId,
        String transformId,
        List<String> sourceFileIds,
        List<String> sourceFileNames,
        String message) {
    public IidmProfileTransformFailed(String importId, String fileId, String message) {
        this(importId, fileId, "", List.of(), List.of(), message);
    }

    public IidmProfileTransformFailed {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        sourceFileNames = sourceFileNames == null ? List.of() : List.copyOf(sourceFileNames);
    }
}
