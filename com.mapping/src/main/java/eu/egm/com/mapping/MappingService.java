package eu.egm.com.mapping;

/**
 * Generic service for configuration-driven object mapping.
 *
 * Implementations should not know about application domains. Domain modules
 * provide {@link MappingDefinition} instances and call this service for the
 * mechanical field transfer.
 */
public interface MappingService {
    <S, T> T map(S source, Class<T> targetType, MappingDefinition definition);

    <S, T> T map(S source, T target, MappingDefinition definition);
}
