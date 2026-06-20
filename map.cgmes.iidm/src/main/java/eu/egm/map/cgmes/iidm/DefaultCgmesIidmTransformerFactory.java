package eu.egm.map.cgmes.iidm;

import eu.egm.com.mapping.MappingService;
import eu.egm.com.mapping.ReflectionMappingService;

/**
 * Default CGMES/IIDM transformer factory backed by reflection-based mapping.
 */
public class DefaultCgmesIidmTransformerFactory implements CgmesIidmTransformerFactory {
    @Override
    public MappingService createMappingService() {
        return new ReflectionMappingService();
    }

    @Override
    public <T extends Transformer<?>> T createTransformer(Class<T> transformerType) {
        if (transformerType.equals(CGMES2IIDMTransformer.class)) {
            return transformerType.cast(new CGMES2IIDMTransformer(createMappingService()));
        }
        if (transformerType.equals(IIDM2CGMESTransformer.class)) {
            return transformerType.cast(new IIDM2CGMESTransformer(createMappingService()));
        }
        throw new IllegalArgumentException("Unsupported transformer type: " + transformerType.getName());
    }
}
