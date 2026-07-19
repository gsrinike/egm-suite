package eu.egm.data.cnm.common;

import java.util.List;

/**
 * JSON-persisted payload containing shared topology and one profile-specific DTO.
 */
public record ProfilePayload<T>(
        ProfileFamily profileFamily,
        String profileType,
        String fileId,
        String objectId,
        List<GridTopologyObject> topologyObjects,
        List<GridTopologyRelation> topologyRelations,
        List<String> warnings,
        T profile) {
    public ProfilePayload {
        topologyObjects = topologyObjects == null ? List.of() : List.copyOf(topologyObjects);
        topologyRelations = topologyRelations == null ? List.of() : List.copyOf(topologyRelations);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
