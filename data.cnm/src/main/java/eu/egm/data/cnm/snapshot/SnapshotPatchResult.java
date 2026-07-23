package eu.egm.data.cnm.snapshot;

import java.util.List;

/**
 * Result of applying an incremental update to a base snapshot.
 */
public record SnapshotPatchResult(
        String baseSnapshotId,
        String newSnapshotId,
        IncrementalUpdateType updateType,
        List<String> affectedMrids,
        List<String> diagnostics) {
    public SnapshotPatchResult {
        updateType = updateType == null ? IncrementalUpdateType.UNKNOWN : updateType;
        affectedMrids = affectedMrids == null ? List.of() : List.copyOf(affectedMrids);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
