package eu.egm.map.cgmes.iidm;

import eu.egm.com.mapping.MappingService;
import eu.egm.com.mapping.ReflectionMappingService;
import eu.egm.com.mapping.transformer.Transformer;

/**
 * Default CGMES/IIDM transformer factory backed by reflection-based mapping.
 */
public class DefaultCgmesIidmTransformerFactory implements CgmesIidmTransformerFactory {
    @Override
    public MappingService createMappingService() {
        return new ReflectionMappingService();
    }

    @Override
    public CgmesIidmMappingConfiguration createMappingConfiguration() {
        return new CgmesIidmMappingConfiguration();
    }

    @Override
    public <T extends Transformer<?>> T createTransformer(Class<T> transformerType) {
        if (transformerType.equals(CGMES2IIDMTransformer.class)) {
            return transformerType.cast(new CGMES2IIDMTransformer(createMappingService(), createMappingConfiguration()));
        }
        if (transformerType.equals(IIDM2CGMESTransformer.class)) {
            return transformerType.cast(new IIDM2CGMESTransformer(createMappingService(), createMappingConfiguration()));
        }
        throw new IllegalArgumentException("Unsupported transformer type: " + transformerType.getName());
    }
}
