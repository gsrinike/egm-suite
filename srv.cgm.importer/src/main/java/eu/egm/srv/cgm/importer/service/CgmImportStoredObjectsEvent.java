package eu.egm.srv.cgm.importer.service;

import java.time.Instant;
import java.util.List;

public record CgmImportStoredObjectsEvent(
        String networkId,
        List<String> objectIds,
        Instant createdAt
) {
}
