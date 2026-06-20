package eu.egm.com.mapping.transformer;

import eu.egm.com.mapping.MappingConfiguration;
import eu.egm.com.mapping.MappingService;

/**
 * Common contract for transformer implementations.
 *
 * @param <T> primary transformed model type
 */
public interface Transformer<T> {
    MappingService mappingService();

    MappingConfiguration mappingConfiguration();
}
