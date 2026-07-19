package eu.egm.data.cnm.common;

import java.util.List;

/**
 * Describes one table whose columns are inferred from profile data.
 */
public record DynamicTableDefinition(
        String tableId,
        String label,
        List<DynamicTableColumn> columns,
        List<DynamicTableRow> rows,
        int totalRows,
        String defaultSort) {
    public DynamicTableDefinition {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
