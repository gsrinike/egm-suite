package com.infra.storage.document;

/**
 * A single storage-neutral search condition.
 *
 * EXACT is intended for keyword-like filters; CONTAINS is intended for free-text
 * searches where partial matches are acceptable.
 */
public record DocumentFilter(String fieldName, Object value, MatchMode matchMode) {
    public enum MatchMode {
        EXACT,
        CONTAINS
    }

    public static DocumentFilter exact(String fieldName, Object value) {
        return new DocumentFilter(fieldName, value, MatchMode.EXACT);
    }

    public static DocumentFilter contains(String fieldName, String value) {
        return new DocumentFilter(fieldName, value, MatchMode.CONTAINS);
    }
}
