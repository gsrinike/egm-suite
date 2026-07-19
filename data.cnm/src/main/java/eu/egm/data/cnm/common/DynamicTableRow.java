package eu.egm.data.cnm.common;

import java.util.Map;

/**
 * Row values keyed by dynamic column id.
 */
public record DynamicTableRow(
        String rowId,
        Map<String, Object> values) {
    public DynamicTableRow {
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
