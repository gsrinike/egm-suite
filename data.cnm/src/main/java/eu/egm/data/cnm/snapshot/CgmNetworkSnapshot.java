package eu.egm.data.cnm.snapshot;

import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.rdf.ProfileFragment;
import eu.egm.data.cnm.state.StateSnapshot;
import eu.egm.data.cnm.topology.StaticTopologyModel;
import java.util.List;

/**
 * Stitched CGMES model snapshot used as the source for IIDM transformation.
 */
public record CgmNetworkSnapshot(
        String snapshotId,
        String importId,
        CnmServiceType serviceType,
        String tsoName,
        String businessDay,
        String businessTime,
        String timeFrame,
        StaticTopologyModel staticTopology,
        StateSnapshot stateSnapshot,
        List<String> sourceFileIds,
        List<String> diagnostics,
        List<ProfileFragment> fragments) {
    public CgmNetworkSnapshot {
        sourceFileIds = sourceFileIds == null ? List.of() : List.copyOf(sourceFileIds);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        fragments = fragments == null ? List.of() : List.copyOf(fragments);
    }
}
