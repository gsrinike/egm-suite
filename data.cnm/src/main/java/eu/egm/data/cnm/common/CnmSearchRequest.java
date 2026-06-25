package eu.egm.data.cnm.common;

/**
 * Paged search request used by GUI and service modules.
 */
public record CnmSearchRequest(
        String query,
        int page,
        int size,
        String sortBy,
        boolean ascending) {
    public CnmSearchRequest {
        page = Math.max(page, 0);
        size = size <= 0 ? 25 : size;
        ascending = sortBy == null || sortBy.isBlank() || ascending;
    }
}
