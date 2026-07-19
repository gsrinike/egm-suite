package eu.egm.data.cnm.common;

import java.util.Map;

/**
 * Association between common topology objects after two-pass RDF resolution.
 */
public record GridTopologyRelation(
        String relationId,
        String sourceMRID,
        String targetMRID,
        String relationType,
        Map<String, Object> attributes) {
    public GridTopologyRelation {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
