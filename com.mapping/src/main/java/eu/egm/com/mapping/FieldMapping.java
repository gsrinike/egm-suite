package eu.egm.com.mapping;

import java.util.Map;

/**
 * Maps one source path to one target path.
 *
 * Paths are dot-separated property names. The optional value mapping is useful
 * for enum/value vocabulary differences between two models.
 */
public record FieldMapping(
        String sourcePath,
        String targetPath,
        Map<String, String> valueMappings
) {
    public FieldMapping {
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalArgumentException("sourcePath is required");
        }
        if (targetPath == null || targetPath.isBlank()) {
            throw new IllegalArgumentException("targetPath is required");
        }
        valueMappings = valueMappings == null ? Map.of() : Map.copyOf(valueMappings);
    }

    public static FieldMapping of(String sourcePath, String targetPath) {
        return new FieldMapping(sourcePath, targetPath, Map.of());
    }

    public static FieldMapping of(String sourcePath, String targetPath, Map<String, String> valueMappings) {
        return new FieldMapping(sourcePath, targetPath, valueMappings);
    }
}
