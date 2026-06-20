package com.infra.document;

/**
 * Describes how an application-owned document type maps to an infrastructure
 * document store. Service modules provide this adapter so the utility module can
 * stay generic and avoid knowing application-specific index names or id rules.
 */
public interface DocumentAdapter<T> {
    /**
     * Physical index or collection name used by the backing store.
     */
    String indexName();

    /**
     * Stable id for upserts. Returning the same id for the same logical document
     * prevents duplicate rows when a document is re-indexed.
     */
    String documentId(T document);

    /**
     * Runtime type used by serialization and mapping frameworks.
     */
    Class<T> documentType();
}
