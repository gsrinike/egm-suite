package eu.egm.data.cgm.dto.cgmes;

import java.time.Instant;
import java.util.List;

public record ImportStatus(
        String networkId,
        String fileName,
        ImportMetadata metadata,
        String state,
        int indexedEquipmentCount,
        Instant createdAt,
        String message,
        String processInstanceId,
        List<ImportFileStatus> files
) {
    public ImportStatus {
        files = files == null ? List.of() : List.copyOf(files);
    }
}
