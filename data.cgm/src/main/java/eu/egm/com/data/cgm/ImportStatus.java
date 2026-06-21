package eu.egm.com.data.cgm;

import java.time.Instant;

public record ImportStatus(
        String networkId,
        String fileName,
        ImportMetadata metadata,
        String state,
        int indexedEquipmentCount,
        Instant createdAt,
        String message
) {
}
