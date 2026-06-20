package eu.egm.map.cgmes.iidm;

import eu.egm.com.data.cgmes.CgmesProcess;
import eu.egm.com.data.cgmes.CgmesRegion;
import eu.egm.com.data.cgmes.EquipmentType;
import eu.egm.com.data.cgmes.EquipmentView;
import eu.egm.com.data.cgmes.ImportMetadata;
import eu.egm.com.data.iidm.IidmEquipment;
import eu.egm.com.data.iidm.IidmEquipmentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IIDM2CGMESTransformerTest {
    private final IIDM2CGMESTransformer transformer =
            new DefaultCgmesIidmTransformerFactory().createTransformer(IIDM2CGMESTransformer.class);

    @Test
    void mapsIidmEquipmentToCgmesEquipmentWithStudyContext() {
        IidmEquipment source = new IidmEquipment("sh-1", "Shunt", IidmEquipmentType.SHUNT_COMPENSATOR, "vl-1", 220.0, Map.of("source", "iidm"));
        ImportMetadata metadata = ImportMetadata.of(LocalDate.parse("2023-10-16"), "00:30", CgmesRegion.CORE, CgmesProcess.CGM);

        EquipmentView result = transformer.transform(source, "network-2", metadata);

        assertThat(result.id()).isEqualTo("sh-1");
        assertThat(result.networkId()).isEqualTo("network-2");
        assertThat(result.metadata()).isSameAs(metadata);
        assertThat(result.type()).isEqualTo(EquipmentType.SHUNT);
    }
}
