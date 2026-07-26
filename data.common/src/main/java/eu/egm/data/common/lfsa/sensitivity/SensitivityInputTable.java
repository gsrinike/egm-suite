package eu.egm.data.common.lfsa.sensitivity;

import java.util.List;
import java.util.Map;

/**
 * Table-ready view of a sensitivity-analysis input file stored in object storage.
 */
public record SensitivityInputTable(
        String kind,
        String objectId,
        List<Map<String, Object>> rows) {
    public SensitivityInputTable {
        kind = kind == null ? "" : kind;
        objectId = objectId == null ? "" : objectId;
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
