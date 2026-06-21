package eu.egm.map.cgm;

import eu.egm.mapping.MappingConfiguration;
import eu.egm.mapping.MappingService;
import eu.egm.mapping.ReflectionMappingService;
import eu.egm.mapping.transformer.Transformer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultCgmesIidmTransformerFactoryTest {
    private final CgmesIidmTransformerFactory factory = new DefaultCgmesIidmTransformerFactory();

    @Test
    void createsReflectionMappingService() {
        MappingService mappingService = factory.createMappingService();

        assertThat(mappingService).isInstanceOf(ReflectionMappingService.class);
    }

    @Test
    void createsCgmesIidmMappingConfiguration() {
        assertThat(factory.createMappingConfiguration()).isInstanceOf(CgmesIidmMappingConfiguration.class);
    }

    @Test
    void createsTransformerFamily() {
        CGMES2IIDMTransformer cgmesToIidmTransformer = factory.createTransformer(CGMES2IIDMTransformer.class);
        IIDM2CGMESTransformer iidmToCgmesTransformer = factory.createTransformer(IIDM2CGMESTransformer.class);

        assertThat(cgmesToIidmTransformer).isInstanceOf(CGMES2IIDMTransformer.class);
        assertThat(cgmesToIidmTransformer.mappingService()).isInstanceOf(ReflectionMappingService.class);
        assertThat(cgmesToIidmTransformer.mappingConfiguration()).isInstanceOf(CgmesIidmMappingConfiguration.class);
        assertThat(iidmToCgmesTransformer).isInstanceOf(IIDM2CGMESTransformer.class);
        assertThat(iidmToCgmesTransformer.mappingService()).isInstanceOf(ReflectionMappingService.class);
        assertThat(iidmToCgmesTransformer.mappingConfiguration()).isInstanceOf(CgmesIidmMappingConfiguration.class);
    }

    @Test
    void rejectsUnsupportedTransformerTypes() {
        assertThatThrownBy(() -> factory.createTransformer(UnsupportedTransformer.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(UnsupportedTransformer.class.getName());
    }

    private static class UnsupportedTransformer implements Transformer<Object> {
        @Override
        public MappingService mappingService() {
            return null;
        }

        @Override
        public MappingConfiguration mappingConfiguration() {
            return null;
        }
    }
}
