package eu.egm.data.cnm.common;

import java.util.List;

/**
 * Generic paged response for CNM UI tables.
 */
public record CnmPage<T>(
        List<T> items,
        long total,
        int page,
        int size) {
    public CnmPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
