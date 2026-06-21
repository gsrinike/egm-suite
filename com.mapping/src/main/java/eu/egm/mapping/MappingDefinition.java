package eu.egm.mapping;

import java.util.List;

/**
 * Configuration object that describes how to map one type to another.
 */
public record MappingDefinition(
        String name,
        Class<?> sourceType,
        Class<?> targetType,
        List<FieldMapping> fields
) {
    public MappingDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (sourceType == null) {
            throw new IllegalArgumentException("sourceType is required");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("targetType is required");
        }
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
