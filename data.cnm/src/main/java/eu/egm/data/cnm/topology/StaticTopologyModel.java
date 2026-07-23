package eu.egm.data.cnm.topology;

import eu.egm.data.cnm.common.GridTopologyObject;
import eu.egm.data.cnm.common.GridTopologyRelation;
import java.util.List;

/**
 * Static equipment/topology layer assembled from profile fragments.
 */
public record StaticTopologyModel(
        List<GridTopologyObject> objects,
        List<GridTopologyRelation> relations,
        List<String> unresolvedReferences) {
    public StaticTopologyModel {
        objects = objects == null ? List.of() : List.copyOf(objects);
        relations = relations == null ? List.of() : List.copyOf(relations);
        unresolvedReferences = unresolvedReferences == null ? List.of() : List.copyOf(unresolvedReferences);
    }
}
