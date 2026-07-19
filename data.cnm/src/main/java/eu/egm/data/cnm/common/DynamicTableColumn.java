package eu.egm.data.cnm.common;

/**
 * Column metadata for generated profile-content tables.
 */
public record DynamicTableColumn(
        String key,
        String label,
        String type,
        boolean sortable,
        boolean searchable,
        String unit) {
}
