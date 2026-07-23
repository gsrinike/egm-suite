package eu.egm.data.cnm.state;

import java.util.List;

/**
 * Timestamped dynamic state overlay for one static topology.
 */
public record StateSnapshot(
        String snapshotId,
        String businessDay,
        String businessTime,
        String timeFrame,
        List<StateVariablePoint> values) {
    public StateSnapshot {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
