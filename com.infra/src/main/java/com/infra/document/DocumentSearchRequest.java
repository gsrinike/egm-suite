package com.infra.document;

import java.util.List;

/**
 * Search request composed of required filters and optional any-match filters.
 *
 * All entries in {@code filters} are AND-ed together. Entries in
 * {@code anyFilters} are OR-ed together, then combined with the required filters.
 */
public record DocumentSearchRequest(
        List<DocumentFilter> filters,
        List<DocumentFilter> anyFilters,
        int page,
        int size
) {
    public DocumentSearchRequest {
        filters = filters == null ? List.of() : List.copyOf(filters);
        anyFilters = anyFilters == null ? List.of() : List.copyOf(anyFilters);
    }
}
