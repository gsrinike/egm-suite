package eu.egm.data.cnm.cgmes;

import java.util.Map;

/**
 * Coordinate system declared by a CGMES Geographical Location profile.
 */
public record CgmesCoordinateSystem(
        String mRID,
        String name,
        String type,
        Map<String, Object> attributes) {
    public CgmesCoordinateSystem {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
