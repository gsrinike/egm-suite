package eu.egm.map.cnm.iidm;

import eu.egm.mapping.MappingService;
import eu.egm.mapping.ReflectionMappingService;
import eu.egm.mapping.transformer.Transformer;
import eu.egm.mapping.transformer.TransformerFactory;

/**
 * Factory for CNM-to-IIDM transformers.
 */
public class CnmToIidmTransformerFactory implements TransformerFactory {
    private final MappingService mappingService;
    private final CnmToIidmMappingConfiguration mappingConfiguration;

    public CnmToIidmTransformerFactory() {
        this(new ReflectionMappingService(), new CnmToIidmMappingConfiguration());
    }

    public CnmToIidmTransformerFactory(
            MappingService mappingService,
            CnmToIidmMappingConfiguration mappingConfiguration) {
        this.mappingService = mappingService;
        this.mappingConfiguration = mappingConfiguration;
    }

    @Override
    public <T extends Transformer<?>> T createTransformer(Class<T> transformerType) {
        if (transformerType == CnmToIidmTransformer.class) {
            return transformerType.cast(new CnmToIidmTransformer(mappingService, mappingConfiguration));
        }
        throw new IllegalArgumentException("Unsupported transformer type: " + transformerType.getName());
    }
}
