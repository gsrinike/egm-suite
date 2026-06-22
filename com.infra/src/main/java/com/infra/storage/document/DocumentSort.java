package com.infra.storage.document;

/**
 * Storage-neutral sort descriptor. Keeping this out of Spring Data types avoids
 * leaking a specific persistence framework into consuming modules.
 */
public record DocumentSort(String fieldName, Direction direction) {
    public enum Direction {
        ASC,
        DESC
    }

    public static DocumentSort descending(String fieldName) {
        return new DocumentSort(fieldName, Direction.DESC);
    }

    public static DocumentSort ascending(String fieldName) {
        return new DocumentSort(fieldName, Direction.ASC);
    }
}
