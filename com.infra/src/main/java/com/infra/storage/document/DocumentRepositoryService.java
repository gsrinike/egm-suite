package com.infra.storage.document;

import java.util.List;

/**
 * Storage-neutral repository contract for document-like records. Implementations
 * may use Elasticsearch, a database, or another backing store, but callers only
 * depend on this small API.
 */
public interface DocumentRepositoryService<T> {
    void save(T document);

    void saveAll(List<T> documents);

    /**
     * Convenience lookup used by callers that need a bounded list for one exact field.
     */
    List<T> findByField(String fieldName, Object value, int maxResults);

    List<T> findAll(int maxResults, DocumentSort sort);

    /**
     * Executes a paged search in the backing store. Use this path for user-facing
     * filters so large datasets are filtered server-side instead of in memory.
     */
    DocumentPage<T> search(DocumentSearchRequest request);
}
