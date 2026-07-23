package eu.egm.data.cnm.snapshot;

import eu.egm.data.cnm.rdf.ProfileFragment;
import java.util.List;

/**
 * Incremental update payload that can be applied to an existing CGM snapshot.
 */
public record IncrementalModelUpdate(
        String updateId,
        String baseSnapshotId,
        IncrementalUpdateType updateType,
        List<String> affectedMrids,
        List<ProfileFragment> fragments) {
    public IncrementalModelUpdate {
        updateType = updateType == null ? IncrementalUpdateType.UNKNOWN : updateType;
        affectedMrids = affectedMrids == null ? List.of() : List.copyOf(affectedMrids);
        fragments = fragments == null ? List.of() : List.copyOf(fragments);
    }
}
