package eu.egm.data.cnm.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Row values keyed by dynamic column id.
 */
public record DynamicTableRow(
        String rowId,
        Map<String, Object> values) {
    public DynamicTableRow {
        values = values == null ? Map.of() : nullSafeCopy(values);
    }

    private static Map<String, Object> nullSafeCopy(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key == null ? "" : key, value == null ? "" : value));
        return Collections.unmodifiableMap(copy);
    }
}
