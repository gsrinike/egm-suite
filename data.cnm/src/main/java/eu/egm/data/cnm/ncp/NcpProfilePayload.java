package eu.egm.data.cnm.ncp;

import java.util.List;
import java.util.Map;

/**
 * NCP profile DTO for profile kinds that do not yet have specialized records.
 */
public record NcpProfilePayload(
        String profileKind,
        List<Map<String, Object>> entities) {
    public NcpProfilePayload {
        entities = entities == null ? List.of() : List.copyOf(entities);
    }
}
