package eu.egm.data.cnm.cgmes;

import java.util.Map;

/**
 * One geographical point in a CGMES Location polyline or point geometry.
 */
public record CgmesPositionPoint(
        String mRID,
        String name,
        String type,
        String locationId,
        Object sequenceNumber,
        Object xPosition,
        Object yPosition,
        Object zPosition,
        Map<String, Object> attributes) {
    public CgmesPositionPoint {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
