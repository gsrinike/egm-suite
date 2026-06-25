package eu.egm.data.cnm.common;

import java.time.Instant;
import java.util.List;

/**
 * Event emitted after an import has been persisted.
 */
public record CnmImportEvent(
        String importId,
        CnmServiceType serviceType,
        TimeFrame timeFrame,
        ImportState state,
        List<CnmProfileMetadata> profiles,
        Instant occurredAt) {
    public CnmImportEvent {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }
}
