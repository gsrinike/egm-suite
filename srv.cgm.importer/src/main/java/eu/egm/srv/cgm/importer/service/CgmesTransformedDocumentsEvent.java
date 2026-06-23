package eu.egm.srv.cgm.importer.service;

import java.time.Instant;
import java.util.List;

public record CgmesTransformedDocumentsEvent(
        String networkId,
        String objectId,
        List<String> documentIds,
        Instant occurredAt
) {
    public CgmesTransformedDocumentsEvent {
        documentIds = documentIds == null ? List.of() : List.copyOf(documentIds);
    }
}
