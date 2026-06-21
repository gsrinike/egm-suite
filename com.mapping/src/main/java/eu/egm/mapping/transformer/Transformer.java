package eu.egm.mapping.transformer;

import eu.egm.mapping.MappingConfiguration;
import eu.egm.mapping.MappingService;

/**
 * Common contract for transformer implementations.
 *
 * @param <T> primary transformed model type
 */
public interface Transformer<T> {
    MappingService mappingService();

    MappingConfiguration mappingConfiguration();
}
