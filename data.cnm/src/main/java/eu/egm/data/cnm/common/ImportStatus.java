package eu.egm.data.cnm.common;

import java.time.Instant;
import java.util.List;

/**
 * Aggregate status for a network-model import.
 */
public record ImportStatus(
        String importId,
        CnmServiceType serviceType,
        TimeFrame timeFrame,
        ImportState state,
        List<ImportFileStatus> files,
        Instant createdAt,
        String message) {
    public ImportStatus {
        files = files == null ? List.of() : List.copyOf(files);
    }
}
