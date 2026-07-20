package eu.egm.srv.iidm.transformer.api;

import eu.egm.data.cnm.common.DynamicTableDefinition;
import java.util.List;

/**
 * IIDM table metadata and, when requested, the current page of table rows.
 */
public record IidmTableBundle(
        String importId,
        String networkId,
        String sourceFileId,
        String tableId,
        int page,
        int size,
        List<DynamicTableDefinition> tables) {
    public IidmTableBundle {
        tables = tables == null ? List.of() : List.copyOf(tables);
    }
}
