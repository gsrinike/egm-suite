package com.infra.storage.document;

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
        List<String> includeFields,
        List<String> excludeFields,
        int page,
        int size
) {
    public DocumentSearchRequest(List<DocumentFilter> filters, List<DocumentFilter> anyFilters, int page, int size) {
        this(filters, anyFilters, List.of(), List.of(), page, size);
    }

    public DocumentSearchRequest {
        filters = filters == null ? List.of() : List.copyOf(filters);
        anyFilters = anyFilters == null ? List.of() : List.copyOf(anyFilters);
        includeFields = includeFields == null ? List.of() : List.copyOf(includeFields);
        excludeFields = excludeFields == null ? List.of() : List.copyOf(excludeFields);
    }
}
