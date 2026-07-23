package eu.egm.data.cnm.nc;

import java.util.List;
import java.util.Map;

/**
 * Network Code profile DTO for profile kinds that do not yet have specialized
 * records. The payload keeps scalar RDF attributes while common grid topology
 * references remain in the surrounding {@code ProfilePayload}.
 */
public record NCProfilePayload(
        NCProfileKind profileKind,
        String profileType,
        List<Map<String, Object>> entities) {
    public NCProfilePayload {
        entities = entities == null ? List.of() : List.copyOf(entities);
    }
}
