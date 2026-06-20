package eu.egm.map.cgmes.iidm;

import eu.egm.com.mapping.MappingService;
import eu.egm.com.mapping.ReflectionMappingService;
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
    void createsTransformerFamily() {
        assertThat(factory.createTransformer(CGMES2IIDMTransformer.class)).isInstanceOf(CGMES2IIDMTransformer.class);
        assertThat(factory.createTransformer(IIDM2CGMESTransformer.class)).isInstanceOf(IIDM2CGMESTransformer.class);
    }

    @Test
    void rejectsUnsupportedTransformerTypes() {
        assertThatThrownBy(() -> factory.createTransformer(UnsupportedTransformer.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(UnsupportedTransformer.class.getName());
    }

    private static class UnsupportedTransformer implements Transformer<Object> {
    }
}
