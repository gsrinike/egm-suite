package eu.egm.com.mapping;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parent type for reusable mapping definition sets.
 */
public class MappingConfiguration {
    private final Map<String, MappingDefinition> definitions = new ConcurrentHashMap<>();

    protected void register(MappingDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition is required");
        }
        definitions.put(definition.name(), definition);
    }

    public MappingDefinition definition(String name) {
        return findDefinition(name)
                .orElseThrow(() -> new MappingException("No mapping definition found for: " + name));
    }

    public Optional<MappingDefinition> findDefinition(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitions.get(name));
    }
}
