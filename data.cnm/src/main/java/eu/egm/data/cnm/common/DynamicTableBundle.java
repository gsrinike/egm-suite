package eu.egm.data.cnm.common;

import java.util.List;

/**
 * Set of dynamic tables generated from a persisted profile payload.
 */
public record DynamicTableBundle(
        String importId,
        String fileId,
        String profileType,
        ProfileFamily profileFamily,
        Object payload,
        List<DynamicTableDefinition> tables) {
    public DynamicTableBundle {
        tables = tables == null ? List.of() : List.copyOf(tables);
    }
}
