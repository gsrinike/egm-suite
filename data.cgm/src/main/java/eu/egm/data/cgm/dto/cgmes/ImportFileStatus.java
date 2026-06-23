package eu.egm.data.cgm.dto.cgmes;

import java.time.Instant;
import java.util.List;

public record ImportFileStatus(
        String objectId,
        String fileName,
        String status,
        String iidmTransformStatus,
        List<String> documentIds,
        String message,
        Instant updatedAt
) {
    public ImportFileStatus {
        documentIds = documentIds == null ? List.of() : List.copyOf(documentIds);
    }
}
