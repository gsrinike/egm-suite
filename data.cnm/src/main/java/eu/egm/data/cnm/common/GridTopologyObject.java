package eu.egm.data.cnm.common;

import java.util.Map;

/**
 * Common topology entity extracted from RDF profiles and reused by profile-specific DTOs.
 */
public record GridTopologyObject(
        String mRID,
        String name,
        String objectType,
        String profileType,
        Map<String, Object> attributes) {
    public GridTopologyObject {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
