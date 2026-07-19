package eu.egm.data.cnm.cgmes;

import java.util.Map;

/**
 * Generic CGMES profile entity with scalar RDF attributes preserved.
 */
public record CgmesProfileEntity(
        String mRID,
        String name,
        String type,
        Map<String, Object> attributes) {
    public CgmesProfileEntity {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
