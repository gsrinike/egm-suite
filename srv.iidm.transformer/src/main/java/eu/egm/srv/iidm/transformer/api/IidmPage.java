package eu.egm.srv.iidm.transformer.api;

import java.util.List;

/**
 * Paged response for IIDM transformer list views.
 */
public record IidmPage<T>(
        List<T> items,
        long total,
        int page,
        int size) {
    public IidmPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
