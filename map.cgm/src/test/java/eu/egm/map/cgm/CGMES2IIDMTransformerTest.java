package eu.egm.map.cgm;

import eu.egm.com.data.cgm.EquipmentType;
import eu.egm.com.data.cgm.EquipmentView;
import eu.egm.com.data.cgm.ImportMetadata;
import eu.egm.com.data.cgm.CgmesProcess;
import eu.egm.com.data.cgm.CgmesRegion;
import eu.egm.com.data.cgm.IidmEquipment;
import eu.egm.com.data.cgm.IidmEquipmentType;
import eu.egm.com.data.cgm.IidmNetwork;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CGMES2IIDMTransformerTest {
    private final CGMES2IIDMTransformer transformer =
            new DefaultCgmesIidmTransformerFactory().createTransformer(CGMES2IIDMTransformer.class);

    @Test
    void mapsCgmesEquipmentToIidmEquipment() {
        EquipmentView source = equipment("tr-1", EquipmentType.TRANSFORMER);

        IidmEquipment result = transformer.transform(source);

        assertThat(result.id()).isEqualTo("tr-1");
        assertThat(result.type()).isEqualTo(IidmEquipmentType.TWO_WINDINGS_TRANSFORMER);
        assertThat(result.attributes()).containsEntry("source", "cgmes");
    }

    @Test
    void mapsEquipmentListToIidmNetwork() {
        IidmNetwork network = transformer.transformNetwork("network-1", List.of(equipment("line-1", EquipmentType.LINE)));

        assertThat(network.id()).isEqualTo("network-1");
        assertThat(network.equipment()).singleElement()
                .extracting(IidmEquipment::type)
                .isEqualTo(IidmEquipmentType.LINE);
    }

    private EquipmentView equipment(String id, EquipmentType type) {
        return new EquipmentView(
                id,
                "network-1",
                ImportMetadata.of(LocalDate.parse("2023-10-16"), "00:30", CgmesRegion.CORE, CgmesProcess.CGM),
                id,
                type,
                "container-1",
                400.0,
                Map.of("source", "cgmes")
        );
    }
}
