package eu.egm.data.cnm.cgmes;

import java.util.Map;

/**
 * CGMES Location resource and its references to the described grid element and
 * coordinate system.
 */
public record CgmesLocation(
        String mRID,
        String name,
        String type,
        String powerSystemResourceId,
        String coordinateSystemId,
        Map<String, Object> attributes) {
    public CgmesLocation {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
